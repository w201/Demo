package ru.itbrick.carwash.ua

import androidx.room.Room
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.badoo.mvicore.extension.SameThreadVerifier
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.itbrick.carwash.ua.api.service.ApiService
import ru.itbrick.carwash.ua.db.AppDatabase
import ru.itbrick.carwash.ua.db.CarwashDao
import ru.itbrick.carwash.ua.feature.carwashes.GetCarWashesFeature
import ru.itbrick.carwash.ua.model.*
import ru.itbrick.carwash.ua.utils.getAuthToken
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit


/**
 * Created by Sergii Volchenko on 16.06.2020.
 * sergey@volchenko.com
 * made in Ukraine
 */
@MediumTest
class DeletingWashCacheTest {

    lateinit var db: AppDatabase
    lateinit var dao: CarwashDao
    val context = InstrumentationRegistry.getInstrumentation().context
    lateinit var api: ApiService
    val disposable = CompositeDisposable()

    object CarWashPool {
        var removed = false
        fun getCarWash(): CarWashModel =
                CarWashModel("1", "testNotDeleted", "123", "456", 2f,
                        Category("1", "1", "asdf"), "sdf", "12:00-13:00", "open",
                        34.3, 34.3, "/", removed, Region("348"), 12)

    }

    @Before
    fun provideDatabase() {
        db = Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java)
                .build()
        dao = db.carwashDao()
        api = mockk(relaxed = true)

        mockkObject(AppDatabase.Companion)
        mockkStatic(AndroidSchedulers::class)
        mockkStatic("ru.itbrick.carwash.ua.utils.SharedPrefsHelperKt")
        mockkStatic(Schedulers::class)

        every { getAuthToken(any()) } answers { "123" }

        every { Schedulers.io() } answers { Schedulers.trampoline() }
        every { AndroidSchedulers.mainThread() } answers { Schedulers.trampoline() }

        every { AppDatabase.newInstance(context) } answers { db }
        every { api.getCarWashesByRegion(any(), any(), any(), any()) } answers { getCarWashesByRegion() }
        every { api.getCityByLatLng(any(), any()) } returns getCityByLatLng()
        every { api.getCarwashesStatuses(any()) } returns getCarWashStatuses()

        SameThreadVerifier.isEnabled = false
        RxJavaPlugins.setErrorHandler { e: Throwable? -> e?.printStackTrace() }
        RxJavaPlugins.setIoSchedulerHandler { schedulerCallable: Scheduler? -> Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { schedulerCallable: Callable<Scheduler?>? -> Schedulers.trampoline() }

    }

    private fun getCarWashStatuses() =
            Observable.just(Response.success(BaseResponse(null, mapOf(Pair("1", "open")))))

    private fun getCityByLatLng() =
            Observable.just(Response.success(BaseResponse(null, CityModel(348, "kiev", "348"))))

    private fun getCarWashesByRegion() =
            Observable.just(Response.success(BaseResponse(null, listOf(CarWashPool.getCarWash()))))

    @Test
    fun deletedWashCacheTest() {

        val feature = GetCarWashesFeature(api, dao)
        val observer = TestObserver<GetCarWashesFeature.News>()

        // Отправляем вначале не удаленную мойку
        CarWashPool.removed = false

        feature.news.subscribe(observer)
        feature.accept(GetCarWashesFeature.Wish.GetCarwashesByRegionWish(34.3, 34.3, 34))
        observer.assertNoValues()

        val washes = dao.getByRegionIdSync(348)
        assertEquals(true, washes.isNotEmpty())
        assertEquals("1", washes[0].id)

        disposable.add(dao.getCarwashByLatLng(34.3, 34.3)
                .subscribeOn(Schedulers.trampoline())
                .observeOn(Schedulers.trampoline())
                .test()
                .assertValueCount(1)
        )

        disposable.add(dao.getAll()
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertValueCount(1)
        )

        disposable.add(
                dao.getAllSingle()
                        .test()
                        .assertValue { it.isNotEmpty() }
        )


        // А теперь удаленную
        CarWashPool.removed = true
        feature.news.subscribe(observer)
        feature.accept(GetCarWashesFeature.Wish.GetCarwashesByRegionWish(34.3, 34.3, 34))
        observer.assertNoValues()

        val washesRemoved = dao.getByRegionIdSync(348)
        assertEquals(true, washesRemoved.isEmpty())

        disposable.add(dao.getCarwashByLatLng(34.3, 34.3)
                .subscribeOn(Schedulers.trampoline())
                .observeOn(Schedulers.trampoline())
                .test()
                .assertNoValues()
        )
        disposable.add(
                dao.getAllSingle()
                        .test()
                        .assertValue { it.isEmpty() }
        )

        disposable.add(dao.getAll()
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertValue { it.isEmpty() }
        )

        observer.dispose()
    }

    @After
    fun clear() {
        if (db.isOpen) db.close()
        disposable.dispose()
    }
}