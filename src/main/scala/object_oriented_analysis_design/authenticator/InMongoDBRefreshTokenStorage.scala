package object_oriented_analysis_design.authenticator

import com.softwaremill.session.{RefreshTokenData, RefreshTokenLookupResult, RefreshTokenStorage}
import object_oriented_analysis_design.api.web.UserSessionData
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

case object InMongoDBRefreshTokenStorage extends RefreshTokenStorage[UserSessionData] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def lookup(selector: String): Future[Option[RefreshTokenLookupResult[UserSessionData]]] = {
    Future.successful {
      val lookupResult = UserSessionData.findBySelector(selector).map { data =>
        RefreshTokenLookupResult[UserSessionData](
          tokenHash = data.tokenHash,
          expires = data.expires,
          createSession = () => data.forSession
        )
      }
      log(s"Looking up token for selector: $selector, found: ${lookupResult.isDefined}")
      lookupResult
    }
  }

  override def store(data: RefreshTokenData[UserSessionData]): Future[Unit] = {
    val iat = System.currentTimeMillis()
    log(s"Storing token for selector: ${data.selector}, user: ${data.forSession}, expires: ${data.expires}, now: ${iat}")
    Future.successful(UserSessionData.save(data, iat))
  }

  override def remove(selector: String) = {
    log(s"Removing token for selector: $selector")
    Future.successful(UserSessionData.remove(selector))
  }

  override def schedule[S](after: Duration)(op: => Future[S]) = {
    log("Running scheduled operation immediately")
    op
    Future.successful(())
  }

  def log(msg: String): Unit = {
    logger.info(msg)
  }
}
