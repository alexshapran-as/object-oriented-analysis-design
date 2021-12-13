package object_oriented_analysis_design.service

import akka.http.scaladsl.server.Directives
import object_oriented_analysis_design.api.web.{CORSSupport, HttpRouteUtils}

case object MainApiService extends HttpRouteUtils with Directives {

    var pathPrefixRole = new String()

    def getRoute =
        CORSSupport.corsHandler {
            extractRequest { req =>
                get("js" / Segment) { sourceName =>
                    getFromResource(s"web/js/$sourceName")
                } ~
                get("css" / Segment) { sourceName =>
                    getFromResource(s"web/css/$sourceName")
                } ~
                get("images" / Segment) { sourceName =>
                    getFromResource(s"web/images/$sourceName")
                } ~
                pathPrefix("auth") {
                    AuthApiService.getRoute(pathPrefixRole)
                } ~
                pathPrefix("admin") {
                    tokenCsrfProtectionDirective {
                        pathPrefixRole = "admin"
                        AdminApiService.getRoute
                    }
                } ~
                pathPrefix("client") {
                    tokenCsrfProtectionDirective {
                        pathPrefixRole = "client"
                        ClientApiService.getRoute
                    }
                }
            }
        }
}
