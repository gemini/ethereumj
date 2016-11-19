package org.ethereum.config;

import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionExecutor;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.datasource.LevelDbDataSource;
import org.ethereum.datasource.mapdb.MapDBFactory;
import org.ethereum.datasource.mapdb.MapDBFactoryImpl;
import org.ethereum.datasource.LegacySourceAdapter;
import org.ethereum.db.RepositoryRoot;
import org.ethereum.db.BlockStore;
import org.ethereum.listener.EthereumListener;
import org.ethereum.validator.*;
import org.ethereum.vm.VM;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@Configuration
@EnableTransactionManagement
@ComponentScan(
        basePackages = "org.ethereum",
        excludeFilters = @ComponentScan.Filter(NoAutoscan.class))
public class CommonConfig {
    private static final Logger logger = LoggerFactory.getLogger("general");

    public static final byte[] FASTSYNC_DB_KEY = HashUtil.sha3("Key in state DB indicating fastsync in progress".getBytes());

    private static CommonConfig defaultInstance;

    public static CommonConfig getDefault() {
        if (defaultInstance == null && !SystemProperties.isUseOnlySpringConfig()) {
            defaultInstance = new CommonConfig();
        }
        return defaultInstance;
    }

    @Bean
    public SystemProperties systemProperties() {
        return SystemProperties.getSpringDefault();
    }

    @Bean
    BeanPostProcessor initializer() {
        return new Initializer();
    }


    @Bean @Primary
    public Repository repository() {
        return new RepositoryRoot(new LegacySourceAdapter(stateDS()));
    }

    @Bean @Scope("prototype")
    public Repository repository(byte[] stateRoot) {
        return new RepositoryRoot(new LegacySourceAdapter(stateDS()), stateRoot);
    }

    @Bean
    @Scope("prototype")
    @Primary
    public KeyValueDataSource keyValueDataSource() {
        String dataSource = systemProperties().getKeyValueDataSource();
        try {
            if ("mapdb".equals(dataSource)) {
                return mapDBFactory().createDataSource();
            } else {
                dataSource = "leveldb";
                return new LevelDbDataSource();
            }
        } finally {
            logger.info(dataSource + " key-value data source created.");
        }
    }

    @Bean
    public KeyValueDataSource stateDS() {
        KeyValueDataSource ret = keyValueDataSource();
        ret.setName("state");
        ret.init();

        if (ret.get(FASTSYNC_DB_KEY) != null) {
            logger.warn("Last fastsync was interrupted. Removing state db...");
            ((LevelDbDataSource)ret).reset();
        }

        return ret;
    }

    @Bean
    @Scope("prototype")
    public TransactionExecutor transactionExecutor(Transaction tx, byte[] coinbase, Repository track, BlockStore blockStore,
                                                   ProgramInvokeFactory programInvokeFactory, Block currentBlock,
                                                   EthereumListener listener, long gasUsedInTheBlock) {
        return new TransactionExecutor(tx, coinbase, track, blockStore, programInvokeFactory,
                currentBlock, listener, gasUsedInTheBlock);
    }

    @Bean
    @Scope("prototype")
    public VM vm() {
        return new VM(systemProperties());
    }

    @Bean
    @Scope("prototype")
    public Program program(byte[] ops, ProgramInvoke programInvoke, Transaction transaction) {
        return new Program(ops, programInvoke, transaction, systemProperties());
    }

    @Bean
    public BlockHeaderValidator headerValidator() {

        List<BlockHeaderRule> rules = new ArrayList<>(asList(
                new GasValueRule(),
                new ExtraDataRule(systemProperties()),
                new ProofOfWorkRule(),
                new GasLimitRule(systemProperties()),
                new BlockHashRule(systemProperties())
        ));

        return new BlockHeaderValidator(rules);
    }

    @Bean
    public ParentBlockHeaderValidator parentHeaderValidator() {

        List<DependentBlockHeaderRule> rules = new ArrayList<>(asList(
                new ParentNumberRule(),
                new DifficultyRule(systemProperties()),
                new ParentGasLimitRule(systemProperties())
        ));

        return new ParentBlockHeaderValidator(rules);
    }

    @Bean
    @Lazy
    public MapDBFactory mapDBFactory() {
        return new MapDBFactoryImpl();
    }
}
