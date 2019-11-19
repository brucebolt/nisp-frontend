/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nisp.controllers.auth

import akka.util.Timeout
import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{spy, when, verify}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Controller, Result}
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes, Name, Retrieval, ~}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, SessionRecordNotFound}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.common.RetrievalOps._
import uk.gov.hmrc.nisp.config.wiring.NispAuthConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen.{Address, Citizen, CitizenDetailsResponse, MCI_EXCLUSION, NOT_FOUND, TECHNICAL_DIFFICULTIES}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthActionSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  class BrokenAuthConnector(exception: Throwable) extends NispAuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  private val mockAuthConnector: NispAuthConnector = mock[NispAuthConnector]

  private val nino: String = new Generator().nextNino.nino
  private val fakeLoginTimes = LoginTimes(DateTime.now(), None)
  private val credentials = Credentials("providerId", "providerType")
  private val simpleRetrievalResults: Future[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments] =
    Future.successful(Some(nino) ~ ConfidenceLevel.L200 ~ Some(credentials) ~ fakeLoginTimes ~ Enrolments(Set.empty))

  private object Stubs {
    def successBlock(request: AuthenticatedRequest[_]): Future[Result] = Future.successful(Ok)
  }

  val ggSignInUrl = "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  implicit val timeout: Timeout = 5 seconds

  "GET /statepension" should {
    "invoke the block when the user details can be retrieved" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(simpleRetrievalResults)

      val citizen = Citizen(Nino(nino), Some("John"), Some("Smith"), new LocalDate(1983, 1, 2))
      val address = Address(Some("Country"))

      val citizenDetailsResponse = CitizenDetailsResponse(citizen, Some(address))
      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Right(citizenDetailsResponse)))

      val stubs = spy(Stubs)

      val authAction = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val request = FakeRequest("", "")
      val result = authAction.invokeBlock(request, stubs.successBlock)
      status(result) shouldBe OK

      val expectedAuthenticatedRequest = AuthenticatedRequest(request,
        NispAuthedUser(Nino(nino),
          citizen.dateOfBirth,
          UserName(Name(citizen.firstName, citizen.lastName)),
          citizenDetailsResponse.address,
          isSa = false),
        AuthDetails(ConfidenceLevel.L200, Some("providerType"), fakeLoginTimes))
      verify(stubs).successBlock(expectedAuthenticatedRequest)
    }
    
    "return 303 when no session" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction = new AuthActionImpl(mockAuthConnector, cds)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }

    "return error for not found user" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(simpleRetrievalResults)

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(NOT_FOUND)))

      val authAction: AuthActionImpl = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      an[InternalServerException] should be thrownBy await(result)
    }

    "return error for technical difficulties" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(simpleRetrievalResults)

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(TECHNICAL_DIFFICULTIES)))

      val authAction: AuthActionImpl = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      an[InternalServerException] should be thrownBy await(result)
    }

    "return redirect for exclusion when NI" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(simpleRetrievalResults)

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(MCI_EXCLUSION)))

      val authAction: AuthActionImpl = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", "a-uri-with-nirecord"), Stubs.successBlock)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "return redirect for exclusion when not NI" in {
    val mockCitizenDetailsService = mock[CitizenDetailsService]

    when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
      .thenReturn(simpleRetrievalResults)

    when(mockCitizenDetailsService.retrievePerson(any())(any()))
      .thenReturn(Future.successful(Left(MCI_EXCLUSION)))

    val authAction: AuthActionImpl = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
    val result = authAction.invokeBlock(FakeRequest("", "a-non-ni-record-uri"), Stubs.successBlock)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
  }
}
