package io.toolsplus.atlassian.connect.play.events

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestProbe
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.events.{AppEvent, AppInstalledEvent, AppUninstalledEvent}
import io.toolsplus.atlassian.connect.play.generators.AtlassianHostGen
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration._
import scala.language.postfixOps

class EventBusSpec extends TestSpec with GuiceOneAppPerSuite with AtlassianHostGen {

  "Given an EventBus" when {

    "accessing event bus" should {

      "always be the same event bus" in {
        val eventBus1 = EventBus
        val eventBus2 = EventBus
        eventBus1 must be theSameInstanceAs eventBus2
      }

    }

    "receiving one ore more events" should {

      "handle a subclass event" in new Context {
        val listener = system.actorOf(Props(new Actor {
          def receive = {
            case e => testProbe.ref ! e
          }
        }))

        EventBus.subscribe(listener, classOf[AppEvent])

        EventBus.publish(installedEvent)
        testProbe.expectMsg(500 millis, installedEvent)

        EventBus.publish(uninstalledEvent)
        testProbe.expectMsg(500 millis, uninstalledEvent)
      }

      "handle an event" in new Context {
        val listener = system.actorOf(Props(new Actor {
          def receive = {
            case e @ AppInstalledEvent(_) => testProbe.ref ! e
          }
        }))

        EventBus.subscribe(listener, classOf[AppInstalledEvent])

        EventBus.publish(installedEvent)
        testProbe.expectMsg(500 millis, installedEvent)
      }

      "handle multiple event" in new Context {
        val listener = system.actorOf(Props(new Actor {
          def receive = {
            case e @ AppInstalledEvent(_) => testProbe.ref ! e
            case e @ AppUninstalledEvent(_) => testProbe.ref ! e
          }
        }))

        EventBus.subscribe(listener, classOf[AppInstalledEvent])
        EventBus.subscribe(listener, classOf[AppUninstalledEvent])
        EventBus.publish(installedEvent)
        EventBus.publish(uninstalledEvent)

        testProbe.expectMsg(500 millis, installedEvent)
        testProbe.expectMsg(500 millis, uninstalledEvent)
      }

      "differentiate between event classes" in new Context {
        val listener = system.actorOf(Props(new Actor {
          def receive = {
            case e @ AppInstalledEvent(_) => testProbe.ref ! e
          }
        }))

        EventBus.subscribe(listener, classOf[AppUninstalledEvent])
        EventBus.publish(uninstalledEvent)

        testProbe.expectNoMessage(500 millis)
      }

      "not handle not subscribed events" in new Context {
        val listener = system.actorOf(Props(new Actor {
          def receive = {
            case e @ AppInstalledEvent(_) => testProbe.ref ! e
          }
        }))

        EventBus.publish(installedEvent)

        testProbe.expectNoMessage(500 millis)
      }

    }

  }

  trait Context {

    /**
      * Play actor system.
      */
    lazy implicit val system = app.injector.instanceOf[ActorSystem]

    /**
      * Test probe.
      */
    lazy val testProbe = TestProbe()

    lazy val someHost = atlassianHostGen.retryUntil(_ => true).sample.get

    lazy val installedEvent = AppInstalledEvent(someHost)

    lazy val uninstalledEvent = AppUninstalledEvent(someHost)

  }

}
