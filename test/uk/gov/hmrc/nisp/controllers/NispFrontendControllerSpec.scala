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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.nisp.controllers
//
//import org.mockito.ArgumentMatchers._
//import org.mockito.Mockito.{verify, when}
//import org.scalatest.mock.MockitoSugar
//import org.scalatestplus.play.OneAppPerSuite
//import org.slf4j.{Logger => Slf4JLogger}
//import play.api.http.Status
//import play.api.inject.bind
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.mvc.Result
//import play.api.test.FakeRequest
//import play.api.{Application, Logger}
//import uk.gov.hmrc.nisp.config.ApplicationGlobalTrait
//import uk.gov.hmrc.nisp.helpers.MockApplicationGlobal
//import uk.gov.hmrc.play.test.UnitSpec
//
//class NispFrontendControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {
//
//  val mockLogger: Slf4JLogger = mock[Slf4JLogger]
//  when(mockLogger.isErrorEnabled).thenReturn(true)
//
//  override lazy val app: Application = GuiceApplicationBuilder()
//    .overrides(bind[Logger].toInstance(new Logger(mockLogger)))
//    .overrides(bind[ApplicationGlobalTrait].toInstance(MockApplicationGlobal))
//    .build()
//
// // def controller = app.injector.instanceOf[NispFrontendController]
//
//  implicit val request = FakeRequest()
//
//  "onError" should {
//    "should log error details" in {
//      val result: Result = controller.onError(new Exception())
//      verify(mockLogger).error(anyString(), any[Exception])
//    }
//
//    "should return an Internal Server Error (500)" in {
//      val result: Result = controller.onError(new Exception())
//      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
//    }
//  }
//
//}
