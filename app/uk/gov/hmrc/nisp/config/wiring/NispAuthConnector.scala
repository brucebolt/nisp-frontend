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

package uk.gov.hmrc.nisp.config.wiring

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}

object NispAuthConnector extends AuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")

  override def http: HttpGet = new HttpGet with WSHttp {
    override protected def appNameConfiguration: Configuration = Play.current.configuration
    override protected def actorSystem: ActorSystem = Play.current.actorSystem
    override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
  }

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}


object NispDelegationConnector extends DelegationConnector with ServicesConfig {
  override protected def mode = Play.current.mode
  override protected def runModeConfiguration = Play.current.configuration
  val serviceUrl = baseUrl("delegation")
  lazy val http = WSHttp
}