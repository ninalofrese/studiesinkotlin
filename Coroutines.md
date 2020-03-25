# Coroutines

É um framework somente disponível no Kotlin e é colocado como uma opção que reduz o número de callbacks em código sequencial. Precisa ser adicionado como uma library no `build.gradle (:app)`

```groovy
dependencies {
  ...
  implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:x.x.x"
  implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:x.x.x"
}
```

Se está usando RxJava, pode integrar com o coroutines usando a [kotlin-coroutines-rx](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive) library.

### O padrão de callback

Um dos padrões para executar tarefas longas são os callbacks. Usando callbacks, é possível iniciar long-running tasks em uma thread de background. Quando a tarefa terminar, o callback é chamado para informar o resultado na main thread.

```kotlin
// Slow request with callbacks
@UiThread
fun makeNetworkRequest() {
    // The slow network request runs on another thread
    slowFetch { result ->
        // When the result is ready, this callback will get the result
        show(result)
    }
    // makeNetworkRequest() exits after calling slowFetch without waiting for the result
}
```

Por este código ter a anotação @UiThread, deve rodar rápido o suficiente para executar na main thread. Isso significa que precisa retornar um valor rapidamente, assim a próxima tela não é atrasada. Contudo, já que `slowFetch` vai levar segundos ou até minutos para completar, a main thread não pode ficar esperando pelo resultado. O callback `show(result)` permite que `slowFetch` rode em uma thread de background e retorne quando estiver pronto.

### Usando coroutines para remover callbacks

Callback é um padrão bem legal, mas tem algumas desvantagens. Código que usa muitos callbacks podem ficar difíceis de ler e mais difíceis ainda de raciocinar. Além disso, callbacks não permitem o uso de algumas ferramentas da linguagem, como as exceções.

É aí que as coroutines do Kotlin entram, convertendo o código baseado em callbacks em código sequencial. Assim, o código fica mais fácil de ler e pode usar algumas features da linguagem, como as exceções. No fim das contas, elas fazem a mesma coisa: esperam até o resultado da tarefa longa estar disponível e continua a execução. Mas no visual o código parece bem diferente.

```kotlin
// slowFetch is main-safe using coroutines
suspend fun slowFetch(): SlowResult { ... }
```

Comparada com o código baseado em callbacks, o código de coroutines alcança o mesmo resultado de desbloquear a thread atual com menos código. Por conta do estilo sequencial, é mais fácil encadear várias tarefas longas sem precisar criar vários callbacks. Por exemplo, o código abaixo pega um resultado de 2 endpoints e salva no database.

```kotlin
// Request data from network and save it to database with coroutines

// Because of the @WorkerThread, this function cannot be called on the
// main thread without causing an error.
@WorkerThread
suspend fun makeNetworkRequest() {
    // slowFetch and anotherFetch are suspend functions
    val slow = slowFetch()
    val another = anotherFetch()
    // save is a regular function and will block this thread
    database.save(slow, another)
}

// slowFetch is main-safe using coroutines
suspend fun slowFetch(): SlowResult { ... }
// anotherFetch is main-safe using coroutines
suspend fun anotherFetch(): AnotherResult { ... }
```

> Os padrões de `async` e `await` de outras linguagens são baseados no coroutines. Se você tem familiaridade com esses termos, a keyword `suspend` é similar à `async`. Contudo, em Kotlin, `await()` é implícito quando uma `suspend` function é chamada
>
> O Kotlin tem um método `Deferred.await()` que é usado para aguardar o resultado de um coroutine iniciado com o builder `async`

## Conceitos

### Suspending functions

É o jeito que o Kotlin marca as funções disponíveis no coroutines. Quando uma coroutine chama uma função marcada como suspensa, ao invés de bloquear até que ela retorne como geralmente uma função normal é chamada, ela suspende a execução até que o resultado esteja pronto e então retoma de onde parou com o resultado. Enquanto está suspensa e esperando pelo resultado, ela desbloqueia a thread que está executando para que outras funções ou coroutines possam executar.

> A keyword `suspend` não especifica em qual thread o código vai rodar. Funções suspensas podem rodar tanto em background threads como na main thread.

### Scopes

No Kotlin, todas as coroutines rodam dentro de um **CoroutineScope**. Um escopo controla o tempo de vida da coroutine durante o seu trabalho. Quando você cancela o trabalho de um escopo, isso cancela todas as coroutines iniciadas neste escopo. No Android, você pode usar um escopo para cancelar todos os coroutines quando, por exemplo, um usuário sai de uma Activity ou um Fragment. Escopos também permitem que você especifique um dispatcher. Um dispatcher controla qual thread vai rodar um coroutine.

Para os coroutines iniciados pela UI, é correto na maioria das vezes iniciá-las no `Dispatchers.Main`, que é a main thread no Android. Uma coroutine iniciada nesta thread não vai bloquear a main thread enquanto estiver suspensa. Como um coroutine de um ViewModel quase sempre atualiza a UI na main thread, iniciar coroutines na main thread geralmente economiza a troca de threads desnecessárias. Um coroutine inidicado na main thread pode trocar de dispatchers a qualquer momento depois de iniciada. Por exemplo, pode usar um outro dispatcher para parsear um resultado JSON fora da main thread.

> **Coroutines oferece main-safety**
>
> Pela facilidade que os coroutines têm de mudar de thread a qualquer hora e passar os resultados de volta para thread original, é uma boa ideia iniciar coroutines relacionadas a UI na main thread.
>
> Libraries como o Room e o Retrofit oferecem **main-safety** por padrão quando são usadas com o coroutines, assim você não precisa gerenciar as threads para fazer chamadas de network ou database. Isso pode simplificar bastante o código.
>
> Contudo, código bloqueante como reordenar uma lista ou ler de um arquivo ainda precisa que explicitamente seja feita a **main-safety** (redirecionar para outras threads), mesmo usando coroutines. Isso também vale quando se está usando um library que não suporta ainda coroutines.

```kotlin
/**
* Mesmo que o repository esteja fazendo chamadas para o network e database, ele usará o  coroutines para expor uma interface main-safety, assim será seguro chamar pela main thread.
*
Por usar o viewModelScope, quando o usuário sai da tela relacionada à ViewModel, o trabalho iniciado por essa coroutine vai ser automaticamente cancelado. Isso significa que não fará chamadas extras no network ou database.
*/
fun refreshTitle() {
        viewModelScope.launch {
            try {
                _spinner.value = true
                repository.refreshTitle()
            } catch (error: TitleRefreshError) {
                _snackBar.value = error.message
            } finally {
                _spinner.value = false
            }
        }
    }
```

Além do CoroutineScope, quando se usa o lifecycle-viewmodel-ktx library, também é possível usar o **viewModelScope**, configurado especialmente para ViewModels e que é adicionada como uma extension function da classe ViewModel. Esse escopo é vinculado ao `Dispatchers.Main` e vai ser cancelado automaticamente quando o ViewModel for limpo. Existe também o 

### Launch (paralelo - não usa resultados)

Inicia uma coroutine no escopo designado. Isso significa que quando o job que foi passado para o escopo for cancelado, todas as coroutines neste job/ escopo serão canceladas.

```kotlin
    private fun updateTaps() {
        // If the user left the Activity before delay returned, 
        // this coroutine will automatically be cancelled when onCleared is called 
        // upon destruction of the ViewModel.
        viewModelScope.launch {
            tapCount++
            delay(1_000) //it's a suspend function
            _taps.postValue("${tapCount} taps")
        }
    }
```

> Quando começar uma coroutine de uma não-coroutine, comece com **launch**. Desta forma, se ele lançar uma exceção não captada, vai ser automaticamente propagada para os handlers. Uma coroutine começada com `async` não vai gerar uma exception para quem chamou até que você use `await`. Contudo, você só pode chamar `await` de dentro de uma coroutine, já que é uma função suspensa.
>
> Uma vez que esteja dentro de uma coroutine, você pode usar `launch` ou `async` para iniciar coroutines filhas. **Use lauch quando não souber qual resultado retornar, e async quando souber.**

Deve ser usado se você não precisa do resultado do método chamado com launch, por exemplo, se enviou apenas um update ou mudar uma cor ou rastreia uma informação sem precisar retornar. Ela não bloqueia a thread pai.

### WithContext (sequência)

Chama o bloco suspenso especificado com o contexto que for designado, suspende até completar e retorna o resultado. Uma coisa importante é que `withContext()` é uma chamada suspensa, então ela não vai para a próxima linha até que esteja completada. E recomendável quando precisa usar a resposta do método chamado, `withContext()` vai esperar por este resultado e não vai bloquear a main thread.

### Async (paralelo - usa resultado)

Deve ser utilizada quando precisar rodar duas ou mais chamadas de network em paralelo, mas precisa aguardar as respostas antes de computar o resultado. Ela bloqueia a thread pai. Um detalhe é que se você usa async, mas não espera um resultado, ele vai funcionar como o launch.

### Exceptions

Exceções em funções suspensas funcionam como erros em funções normais. Se você lança um erro em uma função suspensa, ela vai ser jogada para quem chamou. Então mesmo que elas executem de um jeito diferente, você pode usar blocos `try/ catch` normais para lidar com eles. Isso é útil porque deixa você usar o suporte existente da linguagem para lidar com o erro ao invés de ter que tratar o erro a cada callback. 

### Blocking calls

É inevitável rodar tudo em funções suspensas, sem bloquear thread nenhuma. Um exemplo é este abaixo, que muda a thread para a IO, e chama alguns métodos que bloqueiam a thread, como o `execute()` e o `insertTitle`. O coroutine que chama isso, possivelmente rodando na Dispatchers.Main, vai ser suspensa até que o lambda do `withContext` for completado.

```kotlin
suspend fun refreshTitle() {
   // interact with *blocking* network and IO calls from a coroutine
   withContext(Dispatchers.IO) {
       val result = try {
           // Make network request using a blocking call
           network.fetchNextTitle().execute()
       } catch (cause: Throwable) {
           // If the network throws an exception, inform the caller
           throw TitleRefreshError("Unable to refresh title", cause)
       }
      
       if (result.isSuccessful) {
           // Save it to database
           titleDao.insertTitle(Title(result.body()!!))
       } else {
           // If it's not successful, inform the callback of the error
           throw TitleRefreshError("Unable to refresh title", null)
       }
   }
}
```

O código acima tem 2 importantes diferenças quando comparado com os callbacks:

1. O `withContext()` retorna seu resultado para o Dispatcher que o chamou, neste caso o `Dispatchers.Main`. A versão com callback retornava para a background thread.
2. Quem chama não precisa passar um callback para esta função. Ela pode confiar na suspensão e retomada para ter um resultado ou erro.

### Cancelamento/ interrupção

Cancelamento de coroutine é [cooperativo](https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html). Isso significa que o código precisa checar pelo cancelamento explicitamente, o que acontece geralmente quando você chama as funções de kotlinx-coroutines. Por causa do bloco `withContext` só fazer chamadas do tipo blocking, ela não vai ser cancelada até que haja um retorno de `withContext`.

Para resolver isso, você pode chamar `yield` regularmente para dar a outras coroutines a chance de rodar e verificar o cancelamento. No código acima, é possível adicionar um `yield` entre o request do network e o query do database. Então, se o coroutine for cancelado enquanto estiver fazendo o request no network, não salvará no database.

Você também pode criar cancelamentos explícitos criando interfaces de low-level de coroutines.

### High-order functions

Dá para criar high-order functions que usem coroutines também. É possível criar uma abstração do código exemplificado, que basicamente tem um boilerplate que pode ser usado em outras ocasiões, tipo mostrar um spinner e erros. A solução é criar uma high-order function que vai abstrair esse boilerplate e considera o diferencial em um argumento `block()`, que é um lambda suspenso. Um lambda suspenso permite que sejam chamadas funções suspensas. Um exemplo nativo do Kotlin é com os builders do coroutines `launch` e `runBlocking`

```kotlin
    fun refreshTitle() {
//        viewModelScope.launch {
//            try {
//                _spinner.value = true
//                repository.refreshTitle()
//            } catch (error: TitleRefreshError) {
//                _snackBar.value = error.message
//            } finally {
//                _spinner.value = false
//            }
//        }
        launchDataLoad {
            repository.refreshTitle()
        }
    }
    // abstrai o código anterior (comentado) e cria uma high-order function com um suspend lambda como parâmetro
    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _spinner.value = true
                block()
            } catch (error: TitleRefreshError) {
                _snackBar.value = error.message
            } finally {
                _spinner.value = false
            }
        }
    }
```



## Suporte para Room e Retrofit

> Você não precisa usar `withContext()` para chamar funções suspensas **main-safe**. Por convenção, você deve garantir que as funções suspensas escritas no app são main-safe. Desta forma é seguro chamá-las de qualquer dispatcher, até `Dispatchers.Main`. Este é o caso depois do suporte para Room e Retrofit. Essas funções já são main-safe, não precisa usar novamente o `mainContext()`.

### Room

```kotlin
// add the suspend modifier to the existing insertTitle

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertTitle(title: Title)
```

Quando você transforma o método em suspend, Room vai fazer com que a query seja main-safe e execute em uma background thread (IO) automaticamente. Contudo, isso significa que você só pode chamar essa query de dentro de um coroutine.

### Retrofit

```kotlin
// add suspend modifier to the existing fetchNextTitle
// change return type from Call<String> to String

interface MainNetwork {
   @GET("next_title.json")
   suspend fun fetchNextTitle(): String
}
```

O Retrofit vai automaticamente rodar em uma thread diferente da main, então dá pra chamar diretamente da `Dispatchers.Main`. 

# WorkManager

Para deixar o WorkManager compatível com o Coroutines, é possível usar a classe `CoroutineWorker` que permite que defina o método `doWork()` como uma função suspensa.

```kotlin
override suspend fun doWork(): Result {
    val database = getDatabase(applicationContext)
    val repository = TitleRepository(network, database.titleDao)

    return try {
        repository.refreshTitle()
        Result.success()
    } catch (error: TitleRefreshError) {
        Result.failure()
    }
}
```

Considerando que agora `doWork()` é uma função suspensa, esse código não vai rodar no Executor especificado na configuração anterior do WorkManager, mas sim usará o dispatcher no membro `coroutineContext` (que por padrão é `Dispatchers.Default`)

# Testes

[Exemplos de testes Codelabs](https://github.com/googlecodelabs/kotlin-coroutines/tree/master/coroutines-codelab/finished_code/src/test/java/com/example/android/kotlincoroutines/main)

## RunBlockingTest

A função [runBlockingTest](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-blocking-test.html) sempre vai bloquear quem chama, assim como uma função normal. O coroutine vai rodar sincronizadamente na mesma thread. Você deve evitar **runBlocking** e **runBlockingTest** no código e preferir usar o launch, que vai retornar imediatamente. `runBlockingTest` deve ser usado apenas em testes enquanto executa coroutines na forma de testes controlados, enquanto o `runBlocking` pode ser usado para prover interfaces bloqueantes para as coroutines.

[Mais detalhes sobre runBlockingTest no contexto do Codelabs](https://codelabs.developers.google.com/codelabs/kotlin-coroutines/#9)

## WorkManager

WorkManager tem disponível várias maneiras de testar a classe Worker e na versão 2.1 foi introduzido o [TestListenableWorkerBuilder](https://developer.android.com/reference/androidx/work/testing/TestListenableWorkerBuilder). Antes de começar o teste, dizemos para o WorkManager sobre o factory assim podemos injetar o network fake. O teste em si usa o TestListenableWorkerBuilder para criar o nosso worker assim podemos rodar ele chamando o método `startWork()`

```kotlin
package com.example.android.kotlincoroutines.main


import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RefreshMainDataWorkTest {

    @Test
    fun testRefreshMainDataWork() {
        val fakeNetwork = MainNetworkFake("OK")

        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<RefreshMainDataWork>(context)
                .setWorkerFactory(RefreshMainDataWork.Factory(fakeNetwork))
                .build()

        // Start the work synchronously
        val result = worker.startWork().get()

        assertThat(result).isEqualTo(Result.success())
    }
}
```

[Código completo do teste do Worker](https://github.com/googlecodelabs/kotlin-coroutines/blob/master/coroutines-codelab/finished_code/src/androidTest/java/com/example/android/kotlincoroutines/main/RefreshMainDataWorkTest.kt)

# Palestra do Nelson Glauber

Job: representa uma tarefa ou conjunto de tarefas em execução. A função launch() retorna um job. O job tem um ciclo de vida: novo, ativo, cancelado e completo.

Flow é uma abstração de cold stream, que nada é executado até que o consumidor se registre no fluxo. O async é assíncrono, mas o Flow é reativo, como RxJava.

# Links

- [Utilizando coroutines no Android](https://medium.com/android-dev-br/utilizando-kotlin-coroutines-no-android-c73fcda71e27)
- [Codelabs de coroutines](https://codelabs.developers.google.com/codelabs/kotlin-coroutines/#0)
- [Codelabs de coroutines avançado com Kotlin Flow e LiveData](https://codelabs.developers.google.com/codelabs/advanced-kotlin-coroutines/index.html#0)
- [Melhorar o desempenho do app com coroutines](https://developer.android.com/kotlin/coroutines)
- [Exemplos de testes Codelabs](https://github.com/googlecodelabs/kotlin-coroutines/tree/master/coroutines-codelab/finished_code/src/test/java/com/example/android/kotlincoroutines/main)