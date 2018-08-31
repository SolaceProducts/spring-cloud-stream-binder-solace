# Solace Spring Cloud Stream Binder Configuration Options

This document defines all the Solace-specific binder configuration options supported by the Solace Spring Cloud Stream Binder.

## Inherited Configuration Options

For auto-configuration-related options required for auto-connecting to Solace message brokers, refer to the [JCSMP Spring Boot Auto-Configuration documentation](https://github.com/SolaceProducts/solace-java-spring-boot#configure-the-application-to-use-your-solace-pubsub-service-credentials).

As for for general binder configuration options and properties, refer to the [Spring Cloud Stream Reference Guide](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_configuration_options).

## Solace Binder Configuration Options

### Solace Consumer Properties

The following properties are available for Solace consumers only and must be prefixed with `spring.cloud.stream.solace.bindings.<channelName>.consumer.`.

See [SolaceConsumerProperties](spring-cloud-stream-binder-solace-core\src\main\java\com\solace\spring\cloud\stream\binder\properties\SolaceConsumerProperties.java) for the most updated list.

<dl>
    <dt>prefix</dt>
    <dd>
        <p>Naming prefix for all topics and queues.</p>
        <p>Default: ""</p>
    </dd>
    <dt>queueAccessType</dt>
    <dd>
        <p>Access type for the consumer group queue.</p>
        <p>Default: EndpointProperties.ACCESSTYPE_NONEXCLUSIVE</p>
    </dd>
    <dt>queuePermission</dt>
    <dd>
        <p>Permissions for the consumer group queue.</p>
        <p>Default: EndpointProperties.PERMISSION_CONSUME</p>
    </dd>
    <dt>queueDiscardBehaviour</dt>
    <dd>
        <p>If specified, whether to notify sender if a message fails to be enqueued to the consumer group queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueMaxMsgRedelivery</dt>
    <dd>
        <p>Sets the maximum message redelivery count on consumer group queue. (Zero means retry forever).</p>
        <p>Default: null</p>
    </dd>
    <dt>queueMaxMsgSize</dt>
    <dd>
        <p>Maximum message size for the consumer group queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueQuota</dt>
    <dd>
        <p>Message spool quota for the consumer group queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueRespectsMsgTtl</dt>
    <dd>
        <p>Whether the consumer group queue respects Message TTL.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueAdditionalSubscriptions</dt>
    <dd>
        <p>An array of additional topic subscriptions to be applied on the consumer group queue.</p>
        <p>These subscriptions may also contain wildcards.</p>
        <p>The prefix property is not applied on these subscriptions.</p>
        <p>Default: String[0]</p>
    </dd>
    <dt>anonymousGroupPostfix</dt>
    <dd>
        <p>Naming postfix for the anonymous consumer group queue.</p>
        <p>Default: "anon"</p>
    </dd>
    <dt>polledConsumerWaitTimeInMillis</dt>
    <dd>
        <p>Rate at which polled consumers will receive messages from their consumer group queue.</p>
        <p>Default: 100</p>
    </dd>
    <dt>requeueRejected</dt>
    <dd>
        <p>Whether message processing failures should be re-queued when autoBindDmq is false and after all binder-internal retries have been exhausted.</p>
        <p>Default: false</p>
    </dd>
    <dt>autoBindDmq</dt>
    <dd>
        <p>Whether to automatically create a durable dead message queue to which messages will be republished when message processing failures are encountered. Only applies once all internal retries have been exhausted.</p>
        <p>Default: false</p>
    </dd>
    <dt>dmqAccessType</dt>
    <dd>
        <p>Access type for the DMQ.</p>
        <p>Default: EndpointProperties.ACCESSTYPE_NONEXCLUSIVE</p>
    </dd>
    <dt>dmqPermission</dt>
    <dd>
        <p>Permissions for the DMQ.</p>
        <p>Default: EndpointProperties.PERMISSION_CONSUME</p>
    </dd>
    <dt>dmqDiscardBehaviour</dt>
    <dd>
        <p>If specified, whether to notify sender if a message fails to be enqueued to the DMQ.</p>
        <p>Default: null</p>
    </dd>
    <dt>dmqMaxMsgRedelivery</dt>
    <dd>
        <p>Sets the maximum message redelivery count on the DMQ. (Zero means retry forever).</p>
        <p>Default: null</p>
    </dd>
    <dt>dmqMaxMsgSize</dt>
    <dd>
        <p>Maximum message size for the DMQ.</p>
        <p>Default: null</p>
    </dd>
    <dt>dmqQuota</dt>
    <dd>
        <p>Message spool quota for the DMQ.</p>
        <p>Default: null</p>
    </dd>
    <dt>dmqRespectsMsgTtl</dt>
    <dd>
        <p>Whether the DMQ respects Message TTL.</p>
        <p>Default: null</p>
    </dd>
    <dt>republishedMsgTtl</dt>
    <dd>
        <p>The number of milliseconds before republished messages are discarded or moved to a Solace-internal Dead Message Queue.</p>
        <p>Default: null</p>
    </dd>
</dl>

### Solace Producer Properties

The following properties are available for Solace producers only and must be prefixed with `spring.cloud.stream.solace.bindings.<channelName>.producer.`.

See [SolaceProducerProperties](spring-cloud-stream-binder-solace-core\src\main\java\com\solace\spring\cloud\stream\binder\properties\SolaceProducerProperties.java) for the most updated list.

<dl>
    <dt>prefix</dt>
    <dd>
        <p>Naming prefix for all topics and queues.</p>
        <p>Default: ""</p>
    </dd>
    <dt>queueAccessType</dt>
    <dd>
        <p>Access type for the required consumer group queue.</p>
        <p>Default: EndpointProperties.ACCESSTYPE_NONEXCLUSIVE</p>
    </dd>
    <dt>queuePermission</dt>
    <dd>
        <p>Permissions for the required consumer group queue.</p>
        <p>Default: EndpointProperties.PERMISSION_CONSUME</p>
    </dd>
    <dt>queueDiscardBehaviour</dt>
    <dd>
        <p>If specified, whether to notify sender if a message fails to be enqueued to the required consumer group queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueMaxMsgRedelivery</dt>
    <dd>
        <p>Sets the maximum message redelivery count on the required consumer group queue. (Zero means retry forever).</p>
        <p>Default: null</p>
    </dd>
    <dt>queueMaxMsgSize</dt>
    <dd>
        <p>Maximum message size for the required consumer group queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueQuota</dt>
    <dd>
        <p>Message spool quota for the required consumer group queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueRespectsMsgTtl</dt>
    <dd>
        <p>Whether the required consumer group queue respects Message TTL.</p>
        <p>Default: null</p>
    </dd>
    <dt>queueAdditionalSubscriptions</dt>
    <dd>
        <p>A mapping of required consumer groups to arrays of additional topic subscriptions to be applied on each consumer group's queue.</p>
        <p>These subscriptions may also contain wildcards.</p>
        <p>The prefix property is not applied on these subscriptions.</p>
        <p>Default: Empty Map&lt;String,String[]&gt;</p>
    </dd>
    <dt>msgTtl</dt>
    <dd>
        <p>The number of milliseconds before messages are discarded or moved to a Solace-internal Dead Message Queue.</p>
        <p>Default: null</p>
    </dd>
    <dt>msgInternalDmqEligible</dt>
    <dd>
        <p>The DMQ here is not those which the binder creates when autoBindDmq is enabled, but instead, refers to the <a href="https://docs.solace.com/Configuring-and-Managing/Setting-Dead-Msg-Queues.htm">DMQ defined by the Solace message broker itself</a>.</p>
        <p>Default: false</p>
    </dd>
</dl>