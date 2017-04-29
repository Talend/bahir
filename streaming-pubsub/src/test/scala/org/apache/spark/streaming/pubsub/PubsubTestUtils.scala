/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.streaming.pubsub

import scala.collection.JavaConverters._

import com.google.api.services.pubsub.{Pubsub, PubsubScopes}
import com.google.api.services.pubsub.Pubsub.Builder
import com.google.api.services.pubsub.model.{PublishRequest, PubsubMessage, Subscription, Topic}
import com.google.cloud.hadoop.util.{EntriesCredentialConfiguration, HadoopCredentialConfiguration, RetryHttpInitializer}
import java.util
import org.apache.hadoop.conf.Configuration

import org.apache.spark.internal.Logging

private[pubsub] class PubsubTestUtils extends Logging {

  val projectId = "engineering-152721"
  val serviceAccountJsonPath = "/home/bchen/.ssh/tcomp-integration-test-bigquery.json"
  val serviceAccountEmail = "/home/bchen/.ssh/tcomp-integration-test-bigquery.p12"
  val serviceAccountP12Path =
    "tcomp-integration-test-bigquer@engineering-152721.iam.gserviceaccount.com"
  val APP_NAME = this.getClass.getSimpleName

  val client: Pubsub = {
    new Builder(
      ConnectionUtils.transport,
      ConnectionUtils.jacksonFactory,
      new RetryHttpInitializer(
        new ServiceAccountCredentials(Option(serviceAccountJsonPath)).provider,
        APP_NAME
      ))
    .setApplicationName(APP_NAME)
    .build()
  }

  def createTopic(topic: String): Unit = {
    val topicRequest = new Topic()
    client.projects().topics().create(topic, topicRequest.setName(topic)).execute()
  }

  def createSubscription(topic: String, subscription: String): Unit = {
    val subscriptionRequest = new Subscription()
    client.projects().subscriptions().create(subscription,
      subscriptionRequest.setTopic(topic).setName(subscription)).execute()
  }

  def publishData(topic: String, messages: List[SparkPubsubMessage]): Unit = {
    val publishRequest = new PublishRequest()
    publishRequest.setMessages(messages.map(m => m.message).asJava)
    client.projects().topics().publish(topic, publishRequest).execute()
  }

  def removeSubscription(subscription: String): Unit = {
    client.projects().subscriptions().delete(subscription).execute()
  }

  def removeTopic(topic: String): Unit = {
    client.projects().topics().delete(topic).execute()
  }

  def generatorMessages(num: Int): List[SparkPubsubMessage] = {
    (1 to num)
    .map( n => {
      val m = new PubsubMessage()
      m.encodeData(s"data$n".getBytes)
      m.setAttributes(Map("a1" -> s"v1$n", "a2" -> s"v2$n").asJava)
    })
    .map( m => {
      val sm = new SparkPubsubMessage()
      sm.message = m
      sm
    })
    .toList
  }

  def getFullTopicPath(topic: String): String = s"projects/$projectId/topics/$topic"

  def getFullSubscriptionPath(subscription: String): String = {
    s"projects/$projectId/subscriptions/$subscription"
  }

}
