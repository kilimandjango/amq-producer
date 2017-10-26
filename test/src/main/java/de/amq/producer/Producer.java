package de.amq.producer;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
 
import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class Sender {
  
   public static void main(final String[] args) throws InterruptedException, ExecutionException {
        final long begin = System.currentTimeMillis();
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "failover://ssl://XXX:443");
        final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
        final ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(fixedThreadPool);
        for (int j = 0; j < 100; j++) {
            final int offset = j * 1000;
            final Callable<Void> workItem = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    final Connection connection = connectionFactory.createConnection("XXX", "XXX");
                    connection.start();
                    final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    final Queue queue = session.createQueue("queue/testqueue");
                    final MessageProducer producer = session.createProducer(queue);
                    for (int i = 0; i < 1000; i++) {
                        final TextMessage textMessage = session.createTextMessage(String.format(
                                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                        + "<member><email>amq-%d</email>"
                                        + "<name>AMQ Test</name></member>", offset + i));
                        producer.send(textMessage);
                    }
                    connection.stop();
                    connection.close();
                    return null;
                }
            };
            completionService.submit(workItem);
            System.out.println(String.format("%d workItems submitted", j + 1));
        }
        for (int i = 1; i < 100; i++) {
            final Future<Void> workResult = completionService.take();
            workResult.get();
            System.out.println(String.format("%d messages sent", i * 1000));
        }
        final long end = System.currentTimeMillis();
        System.out.println(String.format("done in %ds", Duration.ofMillis(end - begin).getSeconds()));
        fixedThreadPool.shutdown();
    }
}
