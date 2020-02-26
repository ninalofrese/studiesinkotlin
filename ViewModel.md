## ViewModel Factory

Em alguns casos, você vai querer que um ViewModel já inicialize com algum valor, como por exemplo quando há dados que vêm de um Bundle. Uma maneira de fazer isso é com o ViewModelFactory, que permite que você defina um construtor personalizado para uma ViewModel que é chamada quando você usa o ViewModelProviders. Por padrão, é o ViewModelProviders que instancia o ViewModel com um Factory padrão.

Esses são alguns passos para ter um ViewModelFactory:

- Criar um ViewModel que tem um parâmetro no construtor
- Criar um ViewModelFactory para o ViewModel
- Fazer com que o Factory construa o ViewModel com o parâmetro do construtor
- Passar o ViewModelFactory junto com o ViewModelProviders

```kotlin
class ScoreViewModelFactory(private val finalScore: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScoreViewModel::class.java)){
            return ScoreViewModel(finalScore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ScoreViewModel(finalScore: Int) : ViewModel() {

    private val _score = MutableLiveData<Int>()
    val score: LiveData<Int>
        get() = _score

    init {
        Log.i("ScoreViewModel", "Final score is $finalScore")
        _score.value = finalScore
    }
}

viewModelFactory =
            ScoreViewModelFactory(ScoreFragmentArgs.fromBundle(arguments!!).score)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ScoreViewModel::class.java)
```

## Models

Nessa camada, geralmente se guarda os modelos que serão usados pelo app, entidades do Room e do Retrofit.

> É interessante considerar ter um modelo usado pela UI/ app diferente dos usados pelo Retrofit/ Room. Assim, se a chamada do Retrofit/ Room mudar algum parâmetro, não influencia tão diretamente o funcionamento do app.

## Repository

O repository é uma camada opcional entre data e ViewModels, e pelos exemplos da Udacity, são mais utilizadas para fazer a persistência dos dados da Api no Local.

```kotlin
class VideosRepository(private val database: VideosDatabase) {

    val videos: LiveData<List<Video>> = Transformations.map(database.videoDao.getVideos()) {
        it.asDomainModel()
    }

    suspend fun refreshVideos() {
        withContext(Dispatchers.IO) {
            val playlist = Network.devbytes.getPlaylist().await()
            //como o insertAll usa vararg, ele precisa do spread operator (*), que permite passar
            // uma array para uma função que espera varargs
            database.videoDao.insertAll(*playlist.asDatabaseModel())
        }
    }
}
```

O repository é responsável por cuidar de como os dados serão buscados, sejam de APIs ou de banco de dados locais.