package ua.icherga.feature.carwashes

import com.badoo.mvicore.element.Actor
import com.badoo.mvicore.element.NewsPublisher
import com.badoo.mvicore.element.Reducer
import com.badoo.mvicore.feature.ActorReducerFeature
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import ua.icherga.App
import ua.icherga.Constants.CityConstants.KIEV_CITY_CODE
import ua.icherga.Constants.CityConstants.KIEV_CITY_ID
import ua.icherga.api.service.ApiService
import ua.icherga.api.service.parseResponse
import ua.icherga.db.CarwashDao
import ua.icherga.feature.carwashes.GetCarWashesFeature.*
import ua.icherga.model.CarWashModel
import ua.icherga.utils.addSchedulers
import ua.icherga.utils.getAuthToken

class GetCarWashesFeature(
        apiService: ApiService,
        carwashDao: CarwashDao
) : ActorReducerFeature<Wish, Effect, State, News>(
        initialState = State(),
        actor = ActorImpl(apiService, carwashDao),
        reducer = ReducerImpl(),
        newsPublisher = NewsPublisherImpl()
) {

    data class State(
            val isLoading: Boolean = false,
            val isFilterLoading: Boolean = false,
            val carwashesByLocation: List<CarWashModel>? = null,
            val carwashesByRegion: List<CarWashModel>? = null
    )

    sealed class Wish {
        data class GetCarwashesByRegionWish(val lat: Double, val lng: Double, val carId: Int?) : Wish()
        data class GetCarWashByLatLng(val latitude: Double, val longitude: Double) : Wish()
        data class GetCarWashById(val id: Int) : Wish()
    }

    sealed class Effect {
        object GetCarWashesStart : Effect()
        data class GetCarwashesByLocationSuccess(val list: List<CarWashModel>) : Effect()
        data class GetCarwashesByRegionSuccess(val list: List<CarWashModel>) : Effect()
        data class GetCarWashesFailure(val throwable: Throwable) : Effect()

        object GetCarWashesByQueryStart : Effect()
        data class GetCarWashesByQuerySuccess(val list: List<CarWashModel>) : Effect()
        data class GetCarWashesByQueryFailure(val throwable: Throwable) : Effect()

        object GetCarWashesByLatLngStart : Effect()
        data class GetCarWashesByLatLngSuccess(val carwash: CarWashModel) : Effect()
        data class GetCarWashesByLatLngFailure(val throwable: Throwable) : Effect()

        object GetCarWashesByIdStart : Effect()
        data class GetCarWashesByIdSuccess(val carwash: CarWashModel) : Effect()
        data class GetCarWashesByIdFailure(val throwable: Throwable) : Effect()
    }

    sealed class News {
        data class GetCarWashesFailure(val throwable: Throwable) : News()

        data class GetCarWashesByQuerySuccess(val list: List<CarWashModel>) : News()
        data class GetCarWashesByQueryFailure(val throwable: Throwable) : News()

        data class GetCarWashesByLatLngSuccess(val carwash: CarWashModel) : News()
        data class GetCarWashesByLatLngFailure(val throwable: Throwable) : News()

        data class GetCarWashesByIdSuccess(val carwash: CarWashModel) : News()
        data class GetCarWashesByIdFailure(val throwable: Throwable) : News()
    }

    class ActorImpl(
            private val apiService: ApiService,
            private val carwashDao: CarwashDao
    ) : Actor<State, Wish, Effect> {
        override fun invoke(state: State, wish: Wish): Observable<Effect> = when (wish) {

            is Wish.GetCarwashesByRegionWish ->
                getCarwashesByRegionFromDbBackedWithApi(wish.lat, wish.lng, wish.carId)
                        .map { list -> Effect.GetCarwashesByRegionSuccess(list) as Effect }
                        .startWith(just(Effect.GetCarWashesStart))
                        .onErrorReturn { Effect.GetCarWashesFailure(it) }
             is Wish.GetCarWashesByQueryWish -> {
                 val requests: MutableList<Single<List<CarWashModel>>> = mutableListOf()
                 requests.add(carwashDao.getCarwashesByQueryAndRegionId(wish.query, wish.regionId))
                 if (wish.locationId != null) {
                     requests.add(carwashDao.getCarwashesByQueryAndRegionId(wish.query, wish.locationId))
                 }
                 if (requests.size > 1) {
                     Single.zip(
                             requests[0],
                             requests[1],
                             BiFunction { t1: List<CarWashModel>, t2: List<CarWashModel> ->
                                 t1.union(t2).toList()
                             }
                     )
                 } else {
                     requests.first()
                 }
                         .toObservable()
                         .map { Effect.GetCarWashesByQuerySuccess(it) as Effect }
                         .startWith(just(Effect.GetCarWashesByQueryStart))
                         .onErrorReturn { Effect.GetCarWashesByQueryFailure(it) }
             }
            is Wish.GetCarWashByLatLng ->
                carwashDao.getCarwashByLatLng(wish.latitude, wish.longitude)
                        .toObservable()
                        .map { Effect.GetCarWashesByLatLngSuccess(it) as Effect }
                        .startWith(just(Effect.GetCarWashesByLatLngStart))
                        .onErrorReturn { Effect.GetCarWashesByLatLngFailure(it) }
            is Wish.GetCarWashById ->
                carwashDao.getCarwashById(wish.id)
                        .toObservable()
                        .map { Effect.GetCarWashesByIdSuccess(it) as Effect }
                        .onErrorReturn { Effect.GetCarWashesByIdFailure(it) }
        }.addSchedulers()

        private fun getCarwashesByRegionFromDbBackedWithApi(
                regionLat: Double, regionLon: Double, carId: Int?
        ): Observable<List<CarWashModel>> {
            return apiService.getCityByLatLng(regionLat, regionLon)
                    .flatMap { response ->
                        val parsedResponse = parseResponse(response)
                        getCarwashesFromDb(parsedResponse.first?.id).mergeWith(
                                getCarWashesFromApi(
                                        parsedResponse.first?.id,
                                        parsedResponse.first?.code,
                                        carId
                                ).skipWhile { carwashes ->
                                    try {
                                        val dbCarwashes: List<CarWashModel>? = carwashDao
                                                .getAllSingle()
                                                .subscribeOn(Schedulers.io())
                                                .blockingGet()
                                        // Вставляем измененные и те, которых нет в бд
//                                        get
                                        carwashDao.insertAll(
                                                if (dbCarwashes != null) carwashes - dbCarwashes
                                                else carwashes
                                        )
                                    } catch (e: Exception) {
                                        carwashDao.insertAll(carwashes)
                                    }
                                    // не отдаем результат от сервера, после
                                    // вставки в БД, БД сама отдаст
                                    true
                                }
                        )
                    }
        }

        private fun getCarwashesFromDb(regionId: Int?): Observable<List<CarWashModel>> {
            return if (regionId != null) carwashDao.getByRegionId(regionId) else carwashDao.getAll()
        }

        private fun getCarWashesFromApi(
                cityId: Int?, cityCode: String?, carId: Int?
        ): Observable<List<CarWashModel>> {
            return Observable.zip(
                    getCarwashesByCityId(cityId ?: KIEV_CITY_ID),
                    getCarwashesStatuses(cityCode ?: KIEV_CITY_CODE, carId),
                    BiFunction<List<CarWashModel>, Map<String, String>, List<CarWashModel>> { carwashes, statuses ->
                        carwashes.map { carwashModel ->
                            if (statuses.containsKey(carwashModel.id)) {
                                carwashModel.copy(status = statuses[carwashModel.id])
                            } else {
                                carwashModel
                            }

                        }
                    })
        }

        private fun getCarwashesByCityId(cityId: Int): Observable<List<CarWashModel>> {
            return apiService.getCarWashesByRegion(cityId, 0, getAuthToken(App.appContext!!)!!)
                    .map { response ->
                        val (carwashes, error) = parseResponse(response)
                        carwashes ?: throw error ?: Throwable()
                    }
        }

        private fun getCarwashesStatuses(cityCode: String, carId: Int?): Observable<Map<String, String>> {
            return apiService.getCarwashesStatuses(cityCode)
                    .map { response ->
                        val (carwashes, error) = parseResponse(response)
                        carwashes ?: throw error ?: Throwable()
                    }
        }
    }

    class ReducerImpl : Reducer<State, Effect> {
        override fun invoke(state: State, effect: Effect): State = when (effect) {
            is Effect.GetCarWashesStart -> state.copy(isLoading = true)
            is Effect.GetCarwashesByLocationSuccess -> state.copy(isLoading = false, carwashesByLocation = effect.list)
            is Effect.GetCarwashesByRegionSuccess -> state.copy(isLoading = false, carwashesByRegion = effect.list)
            is Effect.GetCarWashesFailure -> state.copy(isLoading = false)

            is Effect.GetCarWashesByQueryStart -> state.copy(isLoading = true)
            is Effect.GetCarWashesByQuerySuccess -> state.copy(isLoading = false)
            is Effect.GetCarWashesByQueryFailure -> state.copy(isLoading = false)

            is Effect.GetCarWashesByLatLngStart -> state.copy(isLoading = true)
            is Effect.GetCarWashesByLatLngSuccess -> state.copy(isLoading = false)
            is Effect.GetCarWashesByLatLngFailure -> state.copy(isLoading = false)

            is Effect.GetCarWashesByIdStart -> state.copy(isLoading = true)
            is Effect.GetCarWashesByIdSuccess -> state.copy(isLoading = false)
            is Effect.GetCarWashesByIdFailure -> state.copy(isLoading = false)
        }
    }

    class NewsPublisherImpl : NewsPublisher<Wish, Effect, State, News> {
        override fun invoke(wish: Wish, effect: Effect, state: State): News? = when (effect) {
            is Effect.GetCarWashesFailure -> News.GetCarWashesFailure(effect.throwable)

            is Effect.GetCarWashesByQuerySuccess -> News.GetCarWashesByQuerySuccess(effect.list)
            is Effect.GetCarWashesByQueryFailure -> News.GetCarWashesByQueryFailure(effect.throwable)

            is Effect.GetCarWashesByLatLngSuccess -> News.GetCarWashesByLatLngSuccess(effect.carwash)
            is Effect.GetCarWashesByLatLngFailure -> News.GetCarWashesByLatLngFailure(effect.throwable)

            is Effect.GetCarWashesByIdSuccess -> News.GetCarWashesByIdSuccess(effect.carwash)
            is Effect.GetCarWashesByIdFailure -> News.GetCarWashesByIdFailure(effect.throwable)
            else -> null
        }
    }
}
