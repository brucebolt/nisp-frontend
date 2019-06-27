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

package uk.gov.hmrc.nisp.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.config.{ApplicationConfig, ApplicationGlobal, LocalTemplateRenderer}
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}

@Singleton
class GARedirectController @Inject()(val citizenDetailsService: CitizenDetailsService,
                                     val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                     val applicationConfig: ApplicationConfig,
                                     identityVerificationConnector: IdentityVerificationConnector,
                                     formPartialRetriever: FormPartialRetriever,
                                     templateRenderer: LocalTemplateRenderer,
                                     val authConnector: AuthConnector)
                                    extends Actions with AuthorisedForNisp {

  def show: Action[AnyContent] = UnauthorisedAction(
    implicit request =>
      Ok(uk.gov.hmrc.nisp.views.html.gaRedirect()).withNewSession
  )
}
