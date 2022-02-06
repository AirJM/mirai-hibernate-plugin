package xyz.cssxsh.hibernate

import org.hibernate.Session
import org.hibernate.cfg.*
import org.hibernate.dialect.function.*
import org.hibernate.query.*
import org.hibernate.query.criteria.internal.*
import org.hibernate.query.criteria.internal.expression.function.*
import org.hibernate.type.*
import java.sql.*
import javax.persistence.criteria.*

/**
 * rand 函数
 * @see Configuration.addRandFunction
 */
public fun CriteriaBuilder.rand(): BasicFunctionExpression<Double> = RandomFunction(this as CriteriaBuilderImpl)

/**
 * Sqlite 添加 rand 函数别名
 * @see CriteriaBuilder.rand
 */
public fun Configuration.addRandFunction() {
    addSqlFunction("rand", NoArgSQLFunction("random", StandardBasicTypes.DOUBLE))
}

/**
 * 构造一个 Criteria 查询
 */
public inline fun <reified T> Session.withCriteria(block: CriteriaBuilder.(criteria: CriteriaQuery<T>) -> Unit): Query<T> =
    createQuery(with(criteriaBuilder) { createQuery(T::class.java).also { block(it) } })

/**
 * 构造一个 Criteria 查询
 */
public inline fun <reified T> Session.withCriteriaUpdate(block: CriteriaBuilder.(criteria: CriteriaUpdate<T>) -> Unit): Query<*> =
    createQuery(with(criteriaBuilder) { createCriteriaUpdate(T::class.java).also { block(it) } })

public inline fun <reified X> CriteriaQuery<*>.from(): Root<X> = from(X::class.java)

public inline fun <reified T> CriteriaUpdate<T>.from(): Root<T> = from(T::class.java)

/**
 * 获取会话的 [DatabaseMetaData]
 */
public fun Session.getDatabaseMetaData(): DatabaseMetaData = doReturningWork { connection -> connection.metaData }