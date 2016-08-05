package io.toolsplus.atlassian.connect.play.api.repositories

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

import scala.concurrent.Future

trait AtlassianHostRepository {

  /** Retrieves all stored Atlassian Connect hosts.
    *
    * @return List of stored Atlassian Connect hosts.
    */
  def all(): Future[Seq[AtlassianHost]]

  /** Tries to find the host with the given client key.
    *
    * @param clientKey Client key of the Atlassian Connect host.
    * @return Atlassian Connect host, if one is found.
    */
  def findByClientKey(clientKey: ClientKey): Future[Option[AtlassianHost]]

  /** Tries to find the host with the given instance URL.
    *
    * @param instanceUrl Atlassian Connect host instance URL.
    * @return Atlassian Connect host, if one is found.
    */
  def findByBaseUrl(instanceUrl: String): Future[Option[AtlassianHost]]

  /** Saves the given Atlassian Connect host.
    *
    * @param host Atlassian Connect host to store.
    * @return Saved Atlassian Connect host.
    */
  def save(host: AtlassianHost): Future[AtlassianHost]

}
