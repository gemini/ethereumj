package org.ethereum.db;

import org.ethereum.datasource.*;
import org.ethereum.core.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * Storage (tx hash) => (block idx, tx idx, TransactionReceipt)
 *
 * Created by Anton Nashatyrev on 07.04.2016.
 */
@Component
public class TransactionStore extends ObjectDataSource<TransactionInfo> {

    private static final Logger logger = LoggerFactory.getLogger("general");

    private final static Serializer<TransactionInfo, byte[]> serializer =
            new Serializer<TransactionInfo, byte[]>() {
        @Override
        public byte[] serialize(TransactionInfo object) {
            return object.getEncoded();
        }

        @Override
        public TransactionInfo deserialize(byte[] stream) {
            return new TransactionInfo(stream);
        }
    };

    public TransactionStore(KeyValueDataSource src) {
        super(src, serializer);
        withCacheSize(256);
        withCacheOnWrite(true);
    }

    @Override
    public void flush() {
        if (getSrc() instanceof Flushable) {
            ((Flushable) getSrc()).flush();
        }
    }

    @PreDestroy
    public void close() {
        logger.info("destroy(): closing transaction store db");
        getSrc().close();
    }
}
