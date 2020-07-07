package sk.drevari.optitimb.repo.db.dao

import android.util.Log.v
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import sk.drevari.optitimb.data.OTSetting
import sk.drevari.optitimb.repo.data.PatternSortOrder
import sk.drevari.optitimb.repo.db.data.PatternShortInfo
import sk.drevari.optitimb.repo.db.data.PatternWithLog
import sk.drevari.optitimb.repo.db.data.PatternWithLogAndSpecie
import sk.drevari.optitimb.repo.db.entity.Folder
import sk.drevari.optitimb.repo.db.entity.Log
import sk.drevari.optitimb.repo.db.entity.Pattern
import sk.drevari.optitimb.repo.db.entity.PatternLog
import sk.drevari.optitimb.ui.logs.LogFilter
import java.lang.StringBuilder

/**
 * Created by Sergii Volchenko on 27.01.2020.
 * sergey@volchenko.com
 * made in Ukraine
 */
@Dao
interface PatternDao {

    @Insert
    fun insert(pattern: Pattern)

    @Query("DELETE from pattern")
    fun deleteAll(): Int

    @Query("select * from pattern")
    fun getAll(): List<Pattern>

    @Transaction
    @Query("SELECT * from pattern")
    fun getAllWithLog(): LiveData<List<PatternWithLog>>

    @Query("SELECT _id, name, image, rotating, favourite, id_folder from pattern")
    suspend fun getAllShortInfo(): List<PatternShortInfo>

    @Query("SELECT * from pattern where pattern._id = :id")
    fun getPatternWithLog(id: Int): LiveData<PatternWithLog>

    @Query("SELECT * from pattern where pattern._id = :patternId")
    suspend fun getPatterById(patternId: Int): Pattern?

    @Query("SELECT * from pattern where pattern._id = :patternId")
    fun getLivePatterById(patternId: Int): LiveData<Pattern>

    @Query("SELECT * from pattern where pattern._id = :id")
    fun getPatternWithLogAndSpecie(id: Int): LiveData<PatternWithLogAndSpecie>

    @RawQuery(observedEntities = [Pattern::class, PatternLog::class])
    fun getCountOfPatternsForLog(query: SupportSQLiteQuery): LiveData<Int>

    @RawQuery(observedEntities = [Pattern::class, PatternLog::class])
    suspend fun getPatternsForLog(query: SupportSQLiteQuery): List<Pattern>

    fun buildWhereForSearch(logFilter: LogFilter): Pair<String, List<Any>> {
        val args = ArrayList<Any>()
        val queryBuilder = StringBuilder(
            " where pattern.id_log = pattern_log._id "
        )
        logFilter.lengthValue?.let {
            queryBuilder.append(" and (pattern_log.length between ? and ?) ")
            args.add(it)
            args.add(it + OTSetting.logLengthDeviation)
        }

        logFilter.machineId?.let {
            queryBuilder.append(" and (pattern.id_machine = ?) ")
            args.add(it)
        }

        logFilter.diam_big?.let {
            queryBuilder.append(" and pattern_log.diam_big between ? and ? ")
            args.add(it - OTSetting.logDiamDeviation)
            args.add(it + OTSetting.logDiamDeviation)
        }

        logFilter.diam_small?.let {
            queryBuilder.append(" and pattern_log.diam_small between ? and ? ")
            args.add(it - OTSetting.logDiamDeviation)
            args.add(it + OTSetting.logDiamDeviation)
        }
        logFilter.diam_middle?.let {
            queryBuilder.append(" and pattern_log.diam_middle between ? and ? ")
            args.add(it - OTSetting.logDiamDeviation)
            args.add(it + OTSetting.logDiamDeviation)
        }

        logFilter.i_id_wood?.let {
            queryBuilder.append(" and pattern_log.id_wood = ? ")
            args.add(it)
        }

        logFilter.isFavourite?.let {
            queryBuilder.append(" and favourite = ? ")
            args.add(it)
        }
        logFilter.turnover?.let {
            queryBuilder.append(" and rotating = ? ")
            args.add(it)
        }

        logFilter.folder?.let {
            queryBuilder.append(" and id_folder = ? ")
            args.add(it)
        }

        logFilter.partner?.let {
            queryBuilder.append(" and pattern.id_task_client = ?")
            args.add(it)
        }

        logFilter.timbers?.let {
            queryBuilder.append(" and ut.externalId in (").append(it.joinToString { id -> id.toString() }).append(") ")
            queryBuilder.append(" and ut.patternId = pattern._id ")
        }

        return Pair(queryBuilder.toString(), args)
    }

    fun buildWhereForSearch(log: Log, specieId: Long?): Pair<String, List<Any>> {
        val args = ArrayList<Any>()
        val queryBuilder = StringBuilder(
            " where pattern.id_log = pattern_log._id " +
                    " and tally._id = ${log.tallyId} " +
                    " and (pattern_log.length between ? and ?) " +
                    " and (pattern_log.id_wood = $specieId)" +
                    " and (tally.machineId IS NULL or tally.machineId = pattern.id_machine)"
        )
        args.add(log.length!!)
        args.add(log.length!! + OTSetting.logLengthDeviation)


        log.diamBig?.let {
            queryBuilder.append(" and pattern_log.diam_big between ? and ?")
            args.add(it - OTSetting.logDiamDeviation)
            args.add(it + OTSetting.logDiamDeviation)
        }

        log.diamSmall?.let {
            queryBuilder.append(" and pattern_log.diam_small between ? and ?")
            args.add(it - OTSetting.logDiamDeviation)
            args.add(it + OTSetting.logDiamDeviation)
        }
        log.diamMiddle?.let {
            queryBuilder.append(" and pattern_log.diam_middle between ? and ?")
            args.add(it - OTSetting.logDiamDeviation)
            args.add(it + OTSetting.logDiamDeviation)
        }

        return Pair(queryBuilder.toString(), args)
    }

    fun getCountOfPatternsForLog(log: Log, specieId: Long?): LiveData<Int> {

        val queryBuilder = StringBuilder("SELECT count(*) from pattern, pattern_log, tally ")


        val (where, args) = buildWhereForSearch(log, specieId)

        queryBuilder.append(where)
        return getCountOfPatternsForLog(SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray()))
    }

    suspend fun getAllPatternsWithFilter(logFilter: LogFilter, sortOrder: PatternSortOrder?): List<Pattern> {
        val queryBuilder =
            StringBuilder(
                """SELECT pattern.* 
                              from pattern, pattern_log """
            )

        if (logFilter.timbers != null) {
            queryBuilder.append(", used_timber ut ")
        }
        val (where, args) = buildWhereForSearch(logFilter)
        queryBuilder.append(where)

        sortOrder?.let {
            queryBuilder.append(getSortOrder(it))
        }
        return getPatternsForLog(SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray()))

    }

    fun getSortOrder(sortOrder: PatternSortOrder) = when (sortOrder) {
        PatternSortOrder.PRICE -> " order by priceYield desc "
        PatternSortOrder.PROFIT -> " order by volumeYield desc "
    }

    suspend fun getPatternsForLog(log: Log, specieId: Long?): List<Pattern> {

        val queryBuilder =
            StringBuilder(
                """SELECT pattern.*
                                 from pattern, pattern_log, tally """
            )

        val (where, args) = buildWhereForSearch(log, specieId)

        queryBuilder.append(where)
        return getPatternsForLog(SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray()))
    }

    @Query("select count(_id) from pattern")
    fun getCountPattern(): LiveData<Int>

    @Query("select distinct f.* from folder f, pattern p where p.id_folder=f.id order by f.n_order")
    fun getFolders(): LiveData<List<Folder>>

    @Query("select p.* from pattern p, logs l where l.tallyId=:tallyId and l.patternId = p._id")
    suspend fun getPatternsByTallyId(tallyId: Int): List<Pattern>
}