package org.ethereum.db;

import org.ethereum.datasource.*;
import org.ethereum.core.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

/**
 * Storage (tx hash) => List of (block idx, tx idx, TransactionReceipt)
 *
 * Since a transaction could be included into blocks from different forks and
 * have different receipts the class stores all of them (the same manner fork blocks are stored)
 *
 * Created by Anton Nashatyrev on 07.04.2016.
 */
@Component
public class TransactionStore extends ObjectDataSource<List<TransactionInfo>> {

    private static final Logger logger = LoggerFactory.getLogger("general");

    private final static Serializer<List<TransactionInfo>, byte[]> serializer =
            new Serializer<List<TransactionInfo>, byte[]>() {
        @Override
        public byte[] serialize(List<TransactionInfo> object) {
            byte[][] txsRlp = new byte[object.size()][];
            for (int i = 0; i < txsRlp.length; i++) {
                txsRlp[i] = object.get(i).getEncoded();
            }
            return RLP.encodeList(txsRlp);
        }

        @Override
        public List<TransactionInfo> deserialize(byte[] stream) {
            try {
                RLPList params = RLP.decode2(stream);
                RLPList infoList = (RLPList) params.get(0);
                List<TransactionInfo> ret = new ArrayList<>();
                for (int i = 0; i < infoList.size(); i++) {
                    ret.add(new TransactionInfo(infoList.get(i).getRLPData()));
                }
                return ret;
            } catch (Exception e) {
                // fallback to previous DB version
                return Collections.singletonList(new TransactionInfo(stream));
            }
        }
    };

    /**
     * Adds TransactionInfo to the store.
     * If entries for this transaction already exist the method adds new entry to the list
     * if no entry for the same block exists
     * @return true if TransactionInfo was added, false if already exist
     */
    public boolean put(TransactionInfo tx) {
        byte[] txHash = tx.getReceipt().getTransaction().getHash();
        List<TransactionInfo> existingInfos = get(txHash);
        if (existingInfos == null) {
            existingInfos = new ArrayList<>();
        } else {
            for (TransactionInfo info : existingInfos) {
                if (FastByteComparisons.equal(info.getBlockHash(), tx.getBlockHash())) {
                    return false;
                }
            }
        }
        existingInfos.add(tx);
        put(txHash, existingInfos);

        return true;
    }

    public TransactionInfo get(byte[] txHash, byte[] blockHash) {
        List<TransactionInfo> existingInfos = get(txHash);
        for (TransactionInfo info : existingInfos) {
            if (FastByteComparisons.equal(info.getBlockHash(), blockHash)) {
                return info;
            }
        }
        return null;
    }

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
