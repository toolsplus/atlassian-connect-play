package io.toolsplus.atlassian.connect.play.api.repositories

import io.toolsplus.atlassian.connect.play.api.models.ForgeInstallation
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

import scala.concurrent.Future

trait ForgeInstallationRepository {

  /** Retrieves all stored Forge installations.
    *
    * @return List of stored Forge installations.
    */
  def all(): Future[Seq[ForgeInstallation]]

  /** Tries to find the Forge installation with the given installation id.
    *
    * @param installationId Forge installation id
    * @return Forge installation, if one is found.
    */
  def findByInstallationId(
      installationId: String): Future[Option[ForgeInstallation]]

  /** Tries to find the Forge installation by the given client key.
    *
    * @param clientKey Client key of the Atlassian Connect host
    * @return Forge installations, if some are found.
    */
  def findByClientKey(clientKey: ClientKey): Future[Seq[ForgeInstallation]]

  /**
    * Delete all Forge installations associated with a specific clientKey.
    *
    * @param clientKey Client key of the Atlassian Connect host
    * @return Number of deleted Forge installations
    */
  def deleteByClientKey(clientKey: ClientKey): Future[Int]

  /** Saves the given Forge installation.
    *
    * @param installation Forge installation to store.
    * @return Saved Forge installation.
    */
  def save(installation: ForgeInstallation): Future[ForgeInstallation]

}
