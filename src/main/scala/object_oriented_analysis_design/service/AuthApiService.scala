package object_oriented_analysis_design.service

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directives, Route}
import object_oriented_analysis_design.api.web.{HttpRouteUtils, UserSessionData}
import object_oriented_analysis_design.authenticator.AuthApi
import org.slf4j.{Logger, LoggerFactory}

object AuthApiService extends HttpRouteUtils with Directives {
  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  def getRoute(pathPrefix: String): Route =
    respondWithJsonContentType {
      post("signIn") {
        extractPostRequest { case (postStr, postMsa) =>
          val userName = postMsa.getOrElse("userName", throw new IllegalArgumentException("Username was not sent")).toString
          val password = postMsa.getOrElse("loginPassword", throw new IllegalArgumentException("Password was not sent")).toString
          val rememberMe = postMsa.getOrElse("rememberMe", throw new IllegalArgumentException("rememberMe was not sent")).toString.toBoolean
            AuthApi.authorize(userName, password).map { roles =>
              if (!AuthApi.hasAccessToWithRoles(pathPrefix, roles)) complete(getErrorResponse(401, "Unauthorized"))
              else setCsrfToken {
                  if (rememberMe) setRefreshableSession(UserSessionData(userName, roles)) { complete(getOkResponse) }
                  else setOneLogInSession(UserSessionData(userName, roles)) { complete(getOkResponse) }
              }
            }.getOrElse { complete(getErrorResponse(401, "Unauthorized")) }
        }
      } ~
      post("signUp") {
        extractPostRequest { case (postStr, postMsa) =>
          val userName = postMsa.getOrElse("userName", throw new IllegalArgumentException("Username was not sent")).toString
          val password = postMsa.getOrElse("loginPassword", throw new IllegalArgumentException("Password was not sent")).toString
          AuthApi.register(userName, password).map { errMsg => complete(getErrorResponse(500, errMsg)) }.getOrElse { complete(getOkResponse) }
        }
      } ~
      validateRequiredSession { session =>
        post("signOut") {
          invalidateRequiredSession { ctx => ctx.complete(getOkResponse) }
        } ~
        post("changePassword") {
          if (!AuthApi.hasAccessToWithRoles(pathPrefix, session.groups)) reject(AuthorizationFailedRejection)
          else extractPostRequest { case (postStr, postMsa) =>
              val userName = postMsa.getOrElse("userName", throw new IllegalArgumentException("Username was not sent")).toString
              val oldPassword = postMsa.getOrElse("oldPassword", throw new IllegalArgumentException("oldPassword was not sent")).toString
              val newPassword = postMsa.getOrElse("oldPassword", throw new IllegalArgumentException("oldPassword was not sent")).toString
              AuthApi.changePassword(userName, oldPassword, newPassword).map { errMsg => complete(getErrorResponse(500, errMsg)) }.getOrElse { complete(getOkResponse) }
          }
        }
        post("check") {
          if (AuthApi.hasAccessToWithRoles(pathPrefix, session.groups)) complete(getOkResponse(Map("user" -> session.groups.mkString(""))))
          else reject(AuthorizationFailedRejection)
        }
      }
    }
}
