package object_oriented_analysis_design

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import object_oriented_analysis_design.configurations.Conf
import object_oriented_analysis_design.service.MainApiService
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContextExecutor

object Boot {
    protected val logger: Logger = LoggerFactory.getLogger(getClass)

    def main(args: Array[String]): Unit = {
        implicit val system: ActorSystem = ActorSystem("routing-system")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        Http().bindAndHandle(MainApiService.getRoute, Conf.confApiServiceInterface, Conf.confApiServicePort)
        logger.info(s"Company admin page: http://${Conf.confApiServiceInterface}:${Conf.confApiServicePort}/admin/main_page")
        logger.info(s"Shop client page: http://${Conf.confApiServiceInterface}:${Conf.confApiServicePort}/client/main_page")
    }
}
