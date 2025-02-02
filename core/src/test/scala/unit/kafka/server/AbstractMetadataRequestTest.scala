/**
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

package kafka.server

import java.util.Properties
import kafka.network.SocketServer
import kafka.utils.TestUtils
import org.apache.kafka.common.message.MetadataRequestData
import org.apache.kafka.common.protocol.Errors
import org.apache.kafka.common.requests.{MetadataRequest, MetadataResponse}
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig
import org.apache.kafka.server.config.{ServerConfigs, ReplicationConfigs}
import org.junit.jupiter.api.Assertions.assertEquals

abstract class AbstractMetadataRequestTest extends BaseRequestTest {

  override def brokerPropertyOverrides(properties: Properties): Unit = {
    properties.setProperty(GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, "1")
    properties.setProperty(ReplicationConfigs.DEFAULT_REPLICATION_FACTOR_CONFIG, "2")
    properties.setProperty(ServerConfigs.BROKER_RACK_CONFIG, s"rack/${properties.getProperty(ServerConfigs.BROKER_ID_CONFIG)}")
  }

  protected def requestData(topics: List[String], allowAutoTopicCreation: Boolean): MetadataRequestData = {
    val data = new MetadataRequestData
    if (topics == null)
      data.setTopics(null)
    else
      topics.foreach(topic =>
        data.topics.add(
          new MetadataRequestData.MetadataRequestTopic()
            .setName(topic)))

    data.setAllowAutoTopicCreation(allowAutoTopicCreation)
    data
  }

  protected def sendMetadataRequest(request: MetadataRequest, destination: Option[SocketServer] = None): MetadataResponse = {
    connectAndReceive[MetadataResponse](request, destination = destination.getOrElse(anySocketServer))
  }

  protected def checkAutoCreatedTopic(autoCreatedTopic: String, response: MetadataResponse): Unit = {
    assertEquals(Errors.UNKNOWN_TOPIC_OR_PARTITION, response.errors.get(autoCreatedTopic))
    for (i <- 0 until brokers.head.config.numPartitions) {
      TestUtils.waitForPartitionMetadata(brokers, autoCreatedTopic, i)
    }
  }
}
