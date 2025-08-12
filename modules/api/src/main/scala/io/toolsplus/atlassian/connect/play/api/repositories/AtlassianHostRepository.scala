package io.toolsplus.atlassian.connect.play.api.repositories

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

import scala.concurrent.Future

trait AtlassianHostRepository {

  /** Retrieves all stored Atlassian Connect hosts.
    *
    * @return
    *   List of stored Atlassian Connect hosts.
    */
  def all(): Future[Seq[AtlassianHost]]

  /** Tries to find the host with the given client key.
    *
    * @param clientKey
    *   Client key of the Atlassian Connect host.
    * @return
    *   Atlassian Connect host, if one is found.
    */
  def findByClientKey(clientKey: ClientKey): Future[Option[AtlassianHost]]

  /** Retrieves all stored Atlassian Connect hosts that are marked as
    * uninstalled.
    *
    * A host is considered uninstalled if the `installed` flag is set to
    * `false`.
    *
    * @return
    *   List of uninstalled Atlassian Connect hosts.
    */
  def findUninstalled(): Future[Seq[AtlassianHost]]

  /** Saves the given Atlassian Connect host.
    *
    * @param host
    *   Atlassian Connect host to store.
    * @return
    *   Saved Atlassian Connect host.
    */
  def save(host: AtlassianHost): Future[AtlassianHost]

  /** Removes the Atlassian Connect hosts with the given clientKey if it exists
    * and is uninstalled.
    *
    * Implementations should check for `installed=false` as a safeguard to
    * prevent accidental deletion of installed hosts.
    *
    * @param clientKey
    *   Client key of the Atlassian Connect host.
    * @return
    *   Number of affected rows.
    */
  def delete(clientKey: ClientKey): Future[Int]

}
