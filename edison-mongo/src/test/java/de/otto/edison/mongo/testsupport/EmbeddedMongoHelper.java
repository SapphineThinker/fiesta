package de.otto.edison.mongo.testsupport;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.runtime.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.invoke.MethodHandles.lookup;

public class EmbeddedMongoHelper {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongodProcess;
    private static MongoClient mongoClient;
    private static AtomicBoolean started = new AtomicBoolean(false);

    public static void startMongoDB() throws IOException {
        if (!started.compareAndSet(false, true)) {
            throw new RuntimeException("Embedded mongo already running, call stopMongoDB before starting it again!");
        }
        String bindIp = "localhost";
        try {
            int port = Network.getFreeServerPort();
            IMongodConfig mongodConfig = new MongodConfigBuilder()
                    .version(Version.Main.PRODUCTION)
                    .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                    .build();
            final MongodStarter runtime = MongodStarter.getInstance(new RuntimeConfigBuilder()
                    .defaultsWithLogger(Command.MongoD, LOG)
                    .build());
            mongodExecutable = runtime.prepare(mongodConfig, Distribution.detectFor(Version.Main.PRODUCTION));
            mongodProcess = mongodExecutable.start();
            mongoClient = new MongoClient(bindIp, port);
        } catch (IOException e) {
            started.set(false);
            throw e;
        }
    }

    public static void stopMongoDB() {
        if (mongodProcess != null) {
            mongodProcess.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        started.set(false);
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }

}
