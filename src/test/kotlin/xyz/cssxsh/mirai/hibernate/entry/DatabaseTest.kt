package xyz.cssxsh.mirai.hibernate.entry

import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.junit.jupiter.api.*
import xyz.cssxsh.hibernate.*
import java.io.File
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DatabaseTest {

    protected val logger: MiraiLogger = MiraiLogger.Factory.create(this::class.java)

    protected val configuration = Configuration().apply {
        addAnnotatedClass(FaceRecord::class.java)
        addAnnotatedClass(FaceTagRecord::class.java)
        addAnnotatedClass(MessageRecord::class.java)
        addAnnotatedClass(NudgeRecord::class.java)

        setProperty("hibernate.show_sql", "true")
    }

    protected val factory: SessionFactory by lazy {
        File("./data/xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin").mkdirs()
        configuration.addRandFunction()
        configuration.addDiceFunction()
        configuration.buildSessionFactory()
    }

    @BeforeAll
    fun insert() {
        factory.openSession().use { session ->
            session.transaction.begin()
            val random = Random(System.currentTimeMillis())

            repeat(100) { index ->
                val md5 = random.nextLong().toByteArray().md5().toUHexString("")
                val face = FaceRecord(
                    md5 = md5,
                    code = "{}",
                    content = "$index",
                    url = "https://127.0.0.1/$index",
                    height = index,
                    width = index
                )

                session.persist(face)

                val message = MessageRecord(
                    bot = index * 10L,
                    fromId = index * 100L,
                    targetId = index * 1000L,
                    ids = "$index",
                    internalIds ="$index",
                    time = (System.currentTimeMillis() / 1000).toInt(),
                    kind = MessageSourceKind.values().random(),
                    code = md5
                )

                session.persist(message)
            }

            session.transaction.commit()
        }
    }

    @Test
    fun rand() {
        val num = factory.openSession().use { session ->
            session.withCriteria<Double> { criteria ->
                criteria.select(rand())
            }.uniqueResult()
        }
        logger.info("rand $num")
        Assertions.assertTrue(num >= 0.0, "< 0.0")
        Assertions.assertTrue(num <= 1.0, "> 1.0")

        val list = factory.openSession().use { session ->
            session.withCriteria<FaceRecord> { criteria ->
                val record = criteria.from<FaceRecord>()
                criteria.select(record)
                    .orderBy(asc(rand()))
            }.setMaxResults(3).list()
        }
        Assertions.assertEquals(list.size, 3)
    }

    @Test
    fun dice() {
        val num = factory.openSession().use { session ->
            session.withCriteria<Long> { criteria ->
                criteria.select(dice(literal(1000)))
            }.uniqueResult()
        }
        logger.info("dice $num")
        Assertions.assertTrue(num >= 0, "< 0")
        Assertions.assertTrue(num <= 1000, "> 1000")

        val list = factory.openSession().use { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                val id = record.get<Long>("id")
                val max = criteria.subquery<Long>().apply {
                    select(max(from<MessageRecord>().get("id")))
                }

                criteria.select(record)
                    .where(
                        ge(id, dice(max))
                    )
            }.setMaxResults(3).list()
        }
        Assertions.assertEquals(list.size, 3)
    }

    @Test
    fun join() {
        factory.openSession().use { session ->
            val face = session.withCriteria<FaceRecord> { criteria ->
                val root = criteria.from<FaceRecord>()
                criteria.select(root)
            }.setMaxResults(1).uniqueResult()

            session.transaction.begin()
            session.merge(FaceTagRecord(md5 = face.md5, tag = "test"))
            session.transaction.commit()

            logger.info(face.tags.toString())

            session.transaction.begin()
            session.merge(face.copy(disable = true))
            session.transaction.commit()
        }
    }

    @AfterAll
    fun close() {
        factory.close()
    }
}