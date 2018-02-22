package io.toolsplus.atlassian.connect.play.events

import akka.event.{ActorEventBus, SubchannelClassification}
import akka.util.Subclassification
import io.toolsplus.atlassian.connect.play.api.events.AppEvent

/**
  * Event bus implementation which uses a class based lookup classification.
  */
object EventBus extends ActorEventBus with SubchannelClassification {

  override type Classifier = Class[_ <: AppEvent]
  override type Event = AppEvent

  /**
    * Logic to form sub-class hierarchy
    */
  override protected implicit val subclassification
    : Subclassification[Classifier] = new Subclassification[Classifier] {
    def isEqual(x: Classifier, y: Classifier): Boolean = x == y
    def isSubclass(x: Classifier, y: Classifier): Boolean =
      y.isAssignableFrom(x)
  }

  /**
    * Publishes the given Event to the given Subscriber.
    *
    * @param event The Event to publish.
    * @param subscriber The Subscriber to which the Event should be published.
    */
  override protected def publish(event: Event, subscriber: Subscriber): Unit =
    subscriber ! event

  /**
    * Returns the Classifier associated with the given Event.
    *
    * @param event The event for which the Classifier should be returned.
    * @return The Classifier for the given Event.
    */
  override protected def classify(event: Event): Classifier = event.getClass
}