package io.toolsplus.atlassian.connect.play.api.repositories

import io.toolsplus.atlassian.connect.play.api.models.ForgeSystemAccessToken

import java.time.Instant
import scala.concurrent.Future

trait ForgeSystemAccessTokenRepository {

  def all(): Future[Seq[ForgeSystemAccessToken]]

  /** Saves the given Forge system access token by inserting it if it does not
    * exist or updating an existing record if it's already present.
    *
    * @param token
    *   Forge system access token to store.
    * @return
    *   Saved Forge system access token.
    */
  def save(token: ForgeSystemAccessToken): Future[ForgeSystemAccessToken]

  /** Finds the access token for the given installation ID. If an access token
    * is returned, it may be expired.
    *
    * @param installationId
    *   ID of the installation associated with the access token
    * @return
    *   Access token and with metadata, if one is found.
    */
  def findByInstallationId(
      installationId: String
  ): Future[Option[ForgeSystemAccessToken]]

  /** Find the access token that is not yet expired
    *
    * @param installationId
    *   ID of the installation associated with the access token
    * @param expirationTime
    *   In most cases, specify the timestamp as of the moment this method is
    *   called with some leeway
    * @return
    *   Valid access token and with metadata, if one is found.
    */
  def findByInstallationIdAndExpirationTimeAfter(
      installationId: String,
      expirationTime: Instant
  ): Future[Option[ForgeSystemAccessToken]]

  /** Clean up stale records
    *
    * @param expirationTime
    *   In most cases, specify the timestamp as of the moment this method is
    *   called
    * @return
    *   Number of affected rows
    */
  def deleteAllByExpirationTimeBefore(expirationTime: Instant): Future[Int]

}
