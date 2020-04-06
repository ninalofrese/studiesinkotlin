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



| Vantagens                                                    | Desvantagens                                                 |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Código limpo e sequencialmente estruturado                   | Modelo mental completamente diferente                        |
| Tem potencialmente melhor performance em casos de simultaneidade em IO | API mais ou menos documentada                                |
| Menos linhas de código                                       | Integração desafiadora com código bloqueante (blockingQueue é um exemplo) |
|                                                              | Imaturidade                                                  |



## O padrão de callback

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



## Usando coroutines para remover callbacks

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



# Conceitos



## Suspending functions

É o jeito que o Kotlin marca as funções disponíveis no coroutines. Quando uma coroutine chama uma função marcada como suspensa, ao invés de bloquear até que ela retorne como geralmente uma função normal é chamada, ela suspende a execução até que o resultado esteja pronto e então retoma de onde parou com o resultado. Enquanto está suspensa e esperando pelo resultado, ela desbloqueia a thread que está executando para que outras funções ou coroutines possam executar.

> A keyword `suspend` não especifica em qual thread o código vai rodar. Funções suspensas podem rodar tanto em background threads como na main thread.



## Scopes

No Kotlin, todas as coroutines rodam dentro de um **CoroutineScope**. É como um ThreadPool, mas para coroutines. Um escopo controla o tempo de vida da coroutine durante o seu trabalho. Quando você cancela o trabalho de um escopo, isso cancela todas as coroutines iniciadas neste escopo. No Android, você pode usar um escopo para cancelar todos os coroutines quando, por exemplo, um usuário sai de uma Activity ou um Fragment. Escopos também permitem que você especifique um dispatcher. Um dispatcher controla qual thread vai rodar um coroutine.

Para os coroutines iniciados pela UI, é correto na maioria das vezes iniciá-las no `Dispatchers.Main`, que é a main thread no Android. Uma coroutine iniciada nesta thread não vai bloquear a main thread enquanto estiver suspensa. Como um coroutine de um ViewModel quase sempre atualiza a UI na main thread, iniciar coroutines na main thread geralmente economiza a troca de threads desnecessárias. Um coroutine inidicado na main thread pode trocar de dispatchers a qualquer momento depois de iniciada. Por exemplo, pode usar um outro dispatcher para parsear um resultado JSON fora da main thread.

> **Coroutines oferecem main-safety**
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

Quando há um escopo filho criado dentro do pai, ele não é mais responsável pelo cancelamento deste, nem precisa esperar sua execução.



> Scopes também podem receber um job. Então o `scope`  vai usar este Job como parent e desvincular de qualquer outro parent acima.
>
> ```kotlin
> val scope = CoroutineScope(Dispatchers.Main + Job())
> ```



### GlobalScope

Existe também o **globalScope**, que é usado para lançar top-level coroutines que vão operar em todo o tempo de vida da aplicação e não são canceladas prematuramente.

O código do aplicativo geralmente deve usar um CoroutineScope. Usar async ou launch em uma instância de GlobalScope é altamente desencorajado, porque geralmente você não quer coroutines que não sejam canceláveis. Com o GlobalScope, a responsabilidade do tempo de vida das coroutines criadas é totalmente do desenvolvedor. Isso é melhor explicado [aqui](https://medium.com/@elizarov/the-reason-to-avoid-globalscope-835337445abc).

Se você quer que suas coroutines não sejam canceladas, como alternativa pode usar `async(Dispatchers.IO + NonCancellable)` ao invés de `GlobalScope.async(Dispatchers.IO)`.



## Job e Deferred

É uma tarefa em background, cancelável, com um ciclo de vida que culmina em sua conclusão. O `Job()` tem um ciclo de vida: novo, ativo, cancelado e completo.

Os Jobs podem ser organizados em uma hierarquia pai-filho, onde o cancelamento do pai leva ao cancelamento imediato de todos os filhos. A falha ou cancelamento de um filho com uma exceção diferente de `CancellationException` cancela imediatamente seu pai. Por isso, um pai ou mãe pode cancelar seus próprios filhos  sem se cancelar.

As instâncias mais básicas de Job são criadas com o método `launch()` ou com uma função factory de Job. Por padrão, uma falha de qualquer um dos filhos do Job leva a uma falha imediata dos pais e ao cancelamento do restante dos filhos.

Em uma `SupervisorJob()` , a falha de uma filha não afeta as outras. Quando uma falha é notificada, o escopo não faz nada. O supervisor pode implementar um tratamento personalizado para as falhas das filhas:

- A falha de uma filha que foi criada usando *launch* pode ser tratada via CoroutineExceptionHandler no contexto.
- A falha de uma filha que foi criada usando *async* pode ser tratada via Deferred.await no resultado do valor deferido.

Conceitualmente, a execução de um Job não retorna um valor de resultado. O ``Deferred` é um futuro cancelável sem poder ser bloqueado - ou seja, é um Job que tem um resultado. Um `Deferred` é um `Job`. Um Job no coroutineContext de um builder async representa a própria coroutine.



## Launch (paralelo - não usa resultados) retorna Job

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
> Uma vez que esteja dentro de uma coroutine, você pode usar `launch` ou `async` para iniciar coroutines filhas. **Use launch quando não souber qual resultado retornar, e async quando souber.**

Deve ser usado se você não precisa do resultado do método chamado com launch, por exemplo, se enviou apenas um update ou mudar uma cor ou rastreia uma informação sem precisar retornar. Ela não bloqueia a thread pai.



## WithContext (sequência) retorna resultado

Chama o bloco suspenso especificado com o contexto que for designado, suspende até completar e retorna o resultado. Uma coisa importante é que `withContext()` é uma chamada suspensa, então ela não vai para a próxima linha até que esteja completada. E recomendável quando precisa usar a resposta do método chamado, `withContext()` vai esperar por este resultado e não vai bloquear a main thread.



## Async (paralelo - usa resultado) retorna Deferred

Deve ser utilizada quando precisar rodar duas ou mais chamadas de network em paralelo, mas precisa aguardar as respostas antes de computar o resultado. Ela bloqueia a thread pai. Um detalhe é que se você usa async, mas não espera um resultado, ele vai funcionar como o launch.

O `async` trabalha em conjunto com o `await()` ou `awaitAll()`. No exemplo abaixo, tanto os producers como os consumers devem executar paralelamente.

```kotlin
suspend fun startBenchmark() : Result {

  return withContext(Dispatchers.IO) {

    numOfReceivedMessages.set(0)
    numOfProducers.set(0)
    numOfConsumers.set(0)

    val startTimestamp = System.currentTimeMillis()

    // producers init coroutine
    val deferredProducers = async(Dispatchers.IO + NonCancellable) {
      for (i in 0 until NUM_OF_MESSAGES) {
        startNewProducer(i)
      }
    }

    // consumers init coroutine
    val deferredConsumers = async(Dispatchers.IO + NonCancellable) {
      for (i in 0 until NUM_OF_MESSAGES) {
        startNewConsumer()
      }
    }

    awaitAll(deferredConsumers, deferredProducers)

    Result(
      System.currentTimeMillis() - startTimestamp,
      numOfReceivedMessages.get()
    )
  }

}

private fun CoroutineScope.startNewProducer(index: Int) = launch(Dispatchers.IO) {
  Log.d("Producer", "producer ${numOfProducers.incrementAndGet()} started; " +
        "on thread ${Thread.currentThread().name}");
  Thread.sleep(DefaultConfiguration.DEFAULT_PRODUCER_DELAY_MS.toLong())
  blockingQueue.put(index)
}

private fun CoroutineScope.startNewConsumer() = launch(Dispatchers.IO) {
  Log.d("Consumer", "consumer ${numOfConsumers.incrementAndGet()} started; " +
        "on thread ${Thread.currentThread().name}");
  val message = blockingQueue.take()
  if (message != -1) {
    numOfReceivedMessages.incrementAndGet()
  }
}

```







## Context & Dispatchers

O contexto da coroutines inclui um coroutine dispatcher que determina em qual thread ou threads a coroutine vai usar para sua execução. O dispatcher pode isolar uma execução de coroutine para uma thread específica, uma thread pool ou deixa rodar sem limitações. 



**Dispatchers.Main**

O dispatcher que usa a main thread para objetos de UI. Geralmente esse tipo de dispatcher roda em só uma thread.

**Dispatchers.IO**

É o dispatcher designado para ações de entrada e saída. O número de threads usados por esse dispatcher é limitado pelo valor da propriedade de sistema “`kotlinx.coroutines.io.parallelism`” ([IO_PARALLELISM_PROPERTY_NAME](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-i-o_-p-a-r-a-l-l-e-l-i-s-m_-p-r-o-p-e-r-t-y_-n-a-m-e.html)). O padrão é o limite de 64 threads ou o número de núcleos (o que for maior). Além disso, o número máximo configurável de threads é limitado pela propriedade do sistema `kotlinx.coroutines.scheduler.max.pool.size`. Se você precisa de um número maior de threads paralelas, deve usar um dispatcher personalizado apoiado pelo seu próprio thread pool. Esse dispatcher divide threads com o Default dispatcher, então usar `withContext(Dispatchers.IO){}` não leva a mudar de thread, normalmente a execução continua na mesma thread.

```java
// Mudado no onCreate() da Application para threads ilimitadas
System.setProperty(IO_PARALLELISM_PROPERTY_NAME, String.valueOf(Integer.MAX_VALUE));
```

É bem absurdo existir essa limitação nas threads, por isso é sempre recomendável mudar este padrão. Isso deve ser feito com cuidado, porque pode gerar memory leaks. Depois de monitorar as  memory leaks pelo Profiler, 

**Dispatchers.Default**

É o dispatcher padrão usado pelos builders básicos, como launch, async, etc se não houver outro dispatcher especificado em seu contexto. É apoiado por um thread pool compartilhado na JVM. Por padrão, o número máximo de threads usadas por esse dispatcher é igual ao número de CPU cores, mas é pelo menos 2.

**Dispatchers.Unconfined**

Um dispatcher que não é limitado a uma thread específica. Ela executa a continuação inicial de uma coroutine no call-frame atual e permite que a coroutine seja retomada em qualquer thread que for usada pela suspeding function, sem exigir nenhuma política de thread específica. Coroutinas aninhadas lançadas neste dispatcher formam um loop de eventos para evitar stack overflows. Ele executa sequencialmente e na mesma thread.

**Dispatcher personalizado**

Criar um dispatcher personalizado é uma boa maneira de não ter a limitação no número de threads mas, considerando o desenvolvimento em equipes, pode ser que um desenvolvedor não saiba que você criou um dispatcher próprio e acabe alterando novamente para o `Dispatchers.IO()`, porque é uma dispatcher recomendada oficialmente pelo Google. Além disso, bibliotecas podem usar o `Dispatcher.IO()` também. Por isso, melhor mudar o limite padrão da `Dispatchers.IO()` na application (como indicado no código acima).

```kotlin
// Criando um dispatcher personalizado
private val myDispatcher: CoroutineContext = Executors.newCachedThreadPool().asCoroutineDispatcher()
```



Dispatchers são CoroutineContext, mas as contexts têm mais coisas além do dispatcher. Quando se passa um context, ele se torna o pai daquela coroutine, ao invés do pai determinado na hierarquia do código. Cada coroutine builder é uma extensão do CoroutineScope e herda o **coroutineContext** para propagar automaticamente tanto os elementos de contexto e cancelamentos.



```kotlin
val deferredProducers = async(Dispatchers.IO + NonCancellable) {
	for (i in 0 until NUM_OF_MESSAGES) {
		startNewProducer(i)
	}
}
```



- [Documentação sobre context e dispatchers](https://kotlinlang.org/docs/reference/coroutines/coroutine-context-and-dispatchers.html)
- [Desmistificando CoroutineContext](https://proandroiddev.com/demystifying-coroutinecontext-1ce5b68407ad)



## RunBlocking

`RunBlocking`  é uma construção de low-level que bloqueia a thread atual (ela não é uma suspend fun). Ela roda uma nova coroutine e bloqueia a thread atual de forma abrupta (interrompe mesmo) até que seja completada. Esse método não deve ser usado de uma coroutine, ele foi projetado para conectar o código bloqueante com bibliotecas que usam suspending functions, para ser usado em *main* functions e testes. 

Ela transforma uma thread existente em um evento de loop e cria sua coroutine com um Dispatcher que publica coroutines retomadas na fila do loop de eventos. A thread que o chamou permanece dentro dela até que a coroutine seja completada.



### Blocking calls

É inevitável rodar tudo em funções suspensas, sem bloquear thread nenhuma. Um exemplo é este abaixo, que muda a thread para a IO, e chama alguns métodos que bloqueiam a thread, como o `execute()` e o `insertTitle`. O coroutine que chama isso, possivelmente rodando na Dispatchers.Main, vai ser suspenso até que o lambda do `withContext` for completado.

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



## High-order functions

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

## Flow

Um Flow é uma versão assíncrona de Sequence, um tipo de coleção cujos valores são produzidos de forma lazy (o valor é computado apenas no primeiro acesso, nos subsequentes ele é apenas lido). Assim como uma sequência, um flow produz cada valor sob demanda quando ele é requisitado, e os flows podem conter um número infinito de valores. Flow é uma abstração de *cold stream*, que nada é executado até que o consumidor se registre no fluxo. O `async` é assíncrono, mas o Flow é reativo, como RxJava.

A diferença entre Flow e Sequence, é que o Flow conta com o `async` e inclui suporte total ao coroutines. Isso significa que é possível construir, transformar e consumir Flows usando coroutines. Dá também para controlar a concorrência, o que significa coordenar a execução de vários coroutines com Flow.

> `Flow` produz cada valor por vez (ao invés de todos de uma vez) que pode gerar valores de operações async como requisições de network, chamadas de banco de dados ou qualquer outro código async. Tem suporte de coroutines em sua API, por isso dá para transformar um flow usando coroutines também.

É essa funcionalidade do coroutines que mais se aproxima com a potência do RxJava. A lógica pode ser transformada por operadores funcionais como map, flatMapLatest, combine, etc. O Flow também suporta suspending functions na maioria dos operadores. Assim é possivel fazer tarefas sequenciais assíncronas dentro de um operador como o `map`. 



### Como o Flow executa

**A execução é alternada entre o builder e o collect**.

```kotlin
fun makeFlow() = flow {
   println("sending first value")
  // 1 cada vez que o builder chama emit(), ele é suspenso até que o elemento seja completamente processado
   emit(1)
   println("first value collected, sending another value")
  // 2 
   emit(2)
   println("second value collected, sending a third value")
   emit(3)
   println("done")
}

scope.launch {
  // 3 quando outro valor é solicitado do flow (2), ele retoma de onde parou e chama o emit de novo
   makeFlow().collect { value ->
       println("got $value")
   }
  // 4 quando fluxo do builder completa, o Flow é cancelado e o collect é retomada, a coroutina imprime a linha abaixo
   println("flow is completed")
}
```

O operador terminal `collect` é bem importante. O Flow usa operadores suspensos como `collect` ao invés de expor interfaces `Iterators` porque assim sempre sabe quando o flow está sendo ativamente consumidor. Além disso, sabe quando o caller não pode pedir mais valores, e assim limpa os recursos.

> `Flow` foi construído do zero usando coroutines. Usando o mecanismo de `suspend` e `resume`, elas podem sincronizar a execução do producer (`flow`) com o consumer(`collect`). Além disso, o Flow tem o conceito de **backpressure** implementado (como o Flowable do RxJava), que suspende a coroutine.



### Quando o Flow executa

No exemplo acima, o Flow começa a executar quando o operador `collect` executa. Só criar um builder Flow não faz com que nenhum trabalho execute. Ele precisa de um operador terminal, como o collect, ou outros, como `toList`, `first` e  `single`. Por exemplo, o toList vai coletar o flow e adicionar os valores em uma lista.

Por padrão, o Flow será executado:

- Toda vez que um operador terminal é aplicado sem a memória da última execução
- Até que o operador terminal seja cancelado
- Quando o último valor for inteiramente processado, e outro valor ser requisitado

> Essas regras são o comportamento padrão do Flow e é possível fazer como que ele tenha memória, ou seja, não recomece para cada operador terminal, e execute independentemente de ser coletado ou transformado.

Por conta dessas regras, um Flow pode ser usado em estruturas simultâneas, e é seguro criar long-running coroutines com Flow. Não tem chance de um Flow vazar recursos, uma vez que eles são sempre limpos usando as regras de cancelamento de coroutines quando quem chama é cancelado.

```kotlin
scope.launch {
  // 2 com o take(2), o flow só produzirá 2 valores e não vai retomar o lambda de novo depois da segunda chamada do emit(), então a linha "second value collected..." não vai ser printada
   val repeatableFlow = makeFlow().take(2)  // we only care about the first two elements
   println("first collection")
  // 1 o lambda do Flow começa do início cada vez que collect é chamado
   repeatableFlow.collect()
   println("collecting again")
   repeatableFlow.collect()
   println("second collection completed")
}
```

> Por padrão, o Flow vai recomeçar do topo toda vez que um operador terminal for aplicado. Isso é importante se o `Flow` for desempenhar um trabalho mais pesado, como fazer uma chamada de network



### Como o Flow é async?



Usando um exemplo, imagina que precisamos coordenar dados produzidos em um encadeamento no database com observers que estão em outra thread, como a main ou UI. Já que vamos emitir resultados repetidamente conforme os dados mudam, é um cenário perfeito para um padrão sequencial assíncrono.

Imagine que a integração do Room com o Flow seja algo assim:

```kotlin
// This code is a simplified version of how Room implements flow
fun <T> createFlow(query: Query, tables: List<Tables>): Flow<T> = flow {
    val changeTracker = tableChangeTracker(tables)
		
    while(true) {
        emit(suspendQuery(query)) // método main-safe que roda uma suspend query do Room
        changeTracker.suspendUntilChanged() // método que suspende a coroutine até que uma das tabelas mudem
    }
}
```

Quando o Flow acima for coletado, inicialmente vai emitir o primeiro valor do query. Uma vez que isso for processado, o Flow é retomado e chama o `suspendUntilChanged`, que vai fazer exatamente o que o nome diz, vai suspender o Flow até que uma das tabelas mude. Neste ponto, nada está acontecendo no sistema até que uma das tabelas mude e o Flow seja retomado.

Quando isso acontecer, ele fará outra query main-safe e emitirá o resultado. Esse processo vai continuar em um loop infinito. O que pode afetar na performance.

O Flow tem um suporte para simultaneidade estruturada, que lidam bem com esses loops infinitos. A única maneira de consumir valores ou iterar sobre um fluxo é usando um operador terminal. Por todos os operadores terminais serem suspend functions, o trabalho está vinculado ao tempo de vida do escopo que chama o fluxo. Quando o escopo for cancelado, o flow vai automaticamente se cancelar usando as regras de cancelamento cooperativo do coroutines. Então, apenas de escrevermos um loop infinito no nosso builder Flow, podemos consumir sem ter leaks por conta da simultaneidade estruturada.



> Por conta do Flow permitir que consuma valores só com operadores terminais, ele suporta a structured concurrency. Quando o consumidor de um flow for cancelado, todo o Flow será cancelado também. Devido à simultaneidade estruturada, é impossível vaz uma coroutine de uma etapa intermediária.



### Combinando flows declarativamente

Com transformações como `map`, `combine` ou `mapLatest` podemos expressar de forma declarativa como gostaríamos de transformar cada elemento enquanto eles percorrem o flow. É um estilo usado também nas expressões simultâneas, que de forma declarativa podem simplificar o código. 

> Declarativo é um estilo de API que significa descrever o que seu programa deve fazer ao invés de como fazer. Um exemplo comum são as linguagens declarativas SQL.



```kotlin
//COMBINE

private val customSortFlow = plantsListSortOrderCache::getOrAwait.asFlow()

// Ambos os flows vão rodar em suas próprias coroutines, então sempre quando qualquer flow produzir um novo valor a transformação será chamada com  o último valor de cada um dos flows.
val plantsFlow: Flow<List<Plant>>
   get() = plantDao.getPlantsFlow()
       // When the result of customSortFlow is available, this will combine it 
			 // with the latest value from the flow above.  Thus, as long as both 
			 // `plants` and `sortOrder` are have an initial value (their  flow has 	
			 // emitted at least one value), any change to either `plants` or 
			 // `sortOrder`  will call `plants.applySort(sortOrder)`.
       .combine(customSortFlow) { plants, sortOrder ->
          plants.applySort(sortOrder) 
       }
       .flowOn(defaultDispatcher)
       .conflate()
```

O Flow tem main-safety, mas não é por isso que nunca será necessário mudar de thread. Similar ao `subscribeOn()` do RxJava, temos o `flowOn()`. Ele tem 3 efeitos em como o código executa:

1. Lança uma nova coroutine no dispatcher escolhido para executar e coletar o flow **antes** da chamada do flowOn. Ou seja, sempre que tiver um flowOn, o código acima estará sendo executado na thread indicada por ele.
2. Introduz um buffer para enviar resultados da nova corotina para chamadas posteriores.
3. Emite os valores deste buffer no Flow depois do flowOn. No caso do exemplo acima, ´´e o `asLiveData` que chama esse plantsFlow e o transforma em LiveData.

Esse comportamento é muito similar ao do `withContext` para mudar de Dispatcher, mas apresenta um buffer no meio das transformações que mudam o modo que o Flow funciona. A coroutine lançada pelo `flowOn` pode produzir resultados mais rapidamente do que o chamador consome e, por padrão, armazenará em um buffer grande parte deles.

 Neste caso, em que só queremos mandar o resultado para a tela, só precisamos nos preocupar com os resultados mais recentes. É isso o que o operador `conflate` faz - modifica o buffer do flowOn para guardar só o último resultado. Se algum resultado vier antes do anterior ser lido, ele vai ser sobrescrito.

> É importante ter ciência do buffer quando for usar o `flowOn` em objetos muito grandes como resultados do Room, já que é fácil usar muitos recursos de memória carregando resultados.



### Alternando entre dois flows

Ambos os exemplos abaixo fazem a mesma coisa> estão integrando a mudança do growZone no Flow. Eles estão alternando entre 2 data sources baseados em um evento.

```kotlin
   private val growZone = MutableLiveData<GrowZone>(NoGrowZone) 
	 // SWITCHMAP 
	 val plants: LiveData<List<Plant>> = growZone.switchMap { growZone ->
        if (growZone == NoGrowZone) {
            plantRepository.plants
        } else {
            plantRepository.getPlantsWithGrowZone(growZone)
        }
    }

    private val growZoneChannel = ConflatedBroadcastChannel<GrowZone>()
		// FLATMAPLATEST - ExpermimentalCoroutinesAPI
    val plantsUsingFlow: LiveData<List<Plant>> = growZoneChannel.asFlow()
        .flatMapLatest { growZone -> //o mesmo que switchMap - alterna entre flows
            if (growZone == NoGrowZone) {
                plantRepository.plantsFlow
            } else {
                plantRepository.getPlantsWithGrowZoneFlow(growZone)
            }
        }.asLiveData()
```

#### ConflatedBroadcastChannel

Uma novidade neste código é o `ConflatedBroadcastChannel`. Isso é um tipo especial de valor baseado em coroutine que guarda apenas o último valor que foi atribuído. É um primitivo simultâneo thread-safe, então você pode gravar dados nele de múltiplas threads ao mesmo tempo, e a qual for considerada a última vai vencer.

Você também pode dar subscribe para ter atualizações do valor atual. No geral, tem um comportamento similar ao LiveData - ele só guarda o último valor e deixa você observar as mudanças nele. Mas, diferente do LiveData, você precisa usar coroutines para ler os valores em múltiplas threads.

O `ConflatedBroadcastChannel` é frequentemente uma boa maneira de inserir eventos em um flow. Ao juntar duas variantes dos eventos, podemos acompanhar apenas o evento mais recente. Isso é geralmente uma boa escolha, já que os eventos da UI podem ser mais rápidos do que o processamento, e geralmente não ligamos para valores intermediários. Se você precisa passar todos os evetos entre coroutines, mas não quer juntar as variantes, considere usar um `Channel` que é parecido com um `BlockingQueue` , só que usando suspend functions. O builder `channelFlow` pode ser usado para criar flows que suportam channels.

Uma das maneiras de fazer o subscribe no `ConflatedBroadcastChannel` é converter ele em um flow. Isso cria um flow que, quando estiver sendo coletado, vai fazer subscribe às mudanças para o `ConflatedBroadcastChannel` e vai enviá-las pelo fluxo. Ele não adiciona buffers adicionais, então se o coletor do flow for mais lento do que a escrita ao `growZoneChannel`, ele vai pular para emitir apenas o resultado mais recente. Isso é legal porque o cancelamento da subscription do channel vai acontecer quando o flow for cancelado.

Para que um channel saiba sobre a mudança no filtro, usamos o `offer`. Ele é uma function regular (non-suspending) e é uma maneira fácil de comunicar um evento em uma coroutine. Deve ser chamado nos métodos que gerenciam essas mudanças, tipo set e clear.



### Misturando estilos com o flow

Uma das funcionalidades mais legais do Flow é o seu suporte de primeira classe para as suspend functions. O builder flow e quase cada transformação expõe um operador `suspend` que pode chamar qualquer suspending function. Como resultado, tem main-safety para chamadas do network e do banco de dados, assim como orquestra múltiplas operações assíncronas usando chamadas para suspending functions normais de dentro de um flow. Na prática, isso permite que misture transformações declarativas com códigos imperativos. Por exemplo, dentro de um operador map normal é possível usar múltiplas operações async sem aplicar transformações extras. Em vários pontos isso pode levar para um código mais simples do que com a abordagem totalmente declarativa.

> Essa é uma das principais diferenças do Flow para o RxJava.
>
> Enquanto estiver se habituando ao Flow, considere cuidadosamente como pode usar transformações suspensas para simplificar o código. Em vários casos, você pode escrever código assíncrono naturalmente, apoiando em operações suspensas dentro de operadores como `map`, `onStart` e `onCompletion`. Alguns operadores familiares do Rx como combine, mapLatest, flatMapLatest, flattenMerge e flatMapMerge são melhores usados para criar simultaneidade no Flow.



#### Usando suspend functions para criar trabalhos assíncronos

No exemplo abaixo, assim que cada resultado do banco de dados é retornado, vamos receber o sortOrder cacheado e, se não estiver pronta, vai aguardar ao request async do network. Então uma vez que tivermos o sortOrder, é seguro chamar o `applyMainSafeSort`, que vai rodar o sort no dispatcher padrão. Esse código agora é inteiramente main-safe adiando as principais preocupações de segurança para funções de suspensão regulares.

```kotlin
// esse operador map é main-safe mesmo combinando duas operações async
fun getPlantsWithGrowZoneFlow(growZone: GrowZone): Flow<List<Plant>> {
   return plantDao.getPlantsWithGrowZoneNumberFlow(growZone.number) 
       .map { plantList ->
           val sortOrderFromNetwork = plantsListSortOrderCache.getOrAwait() //cache
           val nextValue = plantList.applyMainSafeSort(sortOrderFromNetwork)
           nextValue
       }
}
```



> É **errado** emitir um valor de uma coroutine diferente daquela que chamou a transformação de suspensão. Se você iniciar outra coroutina dentro de um flow, como está sendo feito aqui dentro do `getOrAwait` e `ApplyMainSafeSort`, tenha certeza que o valor é retornado à coroutine original antes de emitir.
>
> Isso vai executar de uma maneira um pouco diferente. O valor cacheado vai ser buscado toda vez que o database emitir um novo valor. Isso é OK porque estamos cacheando corretamente em `plantsListSortOrderCache`, mas se isso começasse um novo request de network, essa implementação faria muitos requests desnecessários. Além do mais, na versão que tem o `.combine`, o request do network e do database rodam simultaneamente, enquanto nesta versão ele roda em sequência.
>
> Por conta destas diferenças, não tem uma regra clara de como estruturar esse código. Em muitos casos, tá tudo bem usar as transformações suspensas como estamos fazendo, o que faz com que todas as operações assíncronas sejam sequenciais. Contudo, em outros casos, é melhor usar operadores para controlar a simultaneidade e dar main-safety.



### Controlar simultaneidade com flow

```kotlin
//lança uma nova coroutine para observar os valores enviados para growZoneChannel
        growZoneChannel.asFlow() //cria um flow do canal
// mapLatest vai aplicar um map em cada valor. Mas, diferente de um map regular, vai lançar uma nova coroutine para cada chamada para o map. Se um novo valor for emitido pelo growZoneChannel antes que o anterior complete, vai cancelar antes de começar um novo.
// Pode ser usado para controlar simultaneidade, porque ao invés de fazer uma lógica de cancelar/restartar, o flow toma conta disso.
            .mapLatest { growZone ->
                _spinner.value = true
                if (growZone == NoGrowZone) {
                    plantRepository.tryUpdateRecentPlantsCache()
                } else {
                    plantRepository.tryUpdateRecentPlantsForGrowZoneCache(growZone)
                }
            }
            // é chamado quando o flow acima dele termina
            .onCompletion {  _spinner.value = false }
            // captura exceções lançadas acima
            .catch { throwable ->  _snackbar.value = throwable.message  }
            .launchIn(viewModelScope) //launch + collect - cria um viewModelScope
```



> **Flow vs. LiveData**
>
> Usando o Flow, é natural coletar data em ViewModel, Repository e outras camadas de dados.
>
> Já que o Flow não é vinculado a UI, você não precisa de um observador para dar um collect em um flow. Essa é uma diferença grande com o LiveData, que sempre precisa de um observador na UI para rodar. Não é uma boa ideia tentar observar um LiveData no ViewModel porque ele não tem um ciclo de vida de observação apropriado.



## Scopes da Lifecycle API

A Lifecycle library possui escopos pré-configurados para que o coroutines possa interagir com a sua API. 



### LifecycleScope

Esse escopo é definido para cada objeto lifecycle e lifecycleOwner. Qualquer coroutina lançada neste escopo é cancelada quando o Lifecycle é destruído. É utilizado em Activity e Fragment. É vinculada à main thread, isso significa que se não for usado um método main safe, é preciso explicitamente mudar de thread.



### ViewModelScope

Além do CoroutineScope, quando se usa o lifecycle-viewmodel-ktx library, também é possível usar o **viewModelScope**, configurado especialmente para ViewModels e que é adicionada como uma extension function da classe ViewModel. Esse escopo é vinculado ao `Dispatchers.Main` e vai ser cancelado automaticamente quando o ViewModel for limpo.



### LiveData

O LiveData é suportado pelo coroutines, o que significa que é possível fazer ações assíncronas, como recuperar as preferências de um usuário e exibir na UI. A função do builder `liveData{}` pode chamar uma suspend function, exibindo o resultado como um objeto LiveData. Ele serve como uma **concorrência estruturada primitiva** entre as coroutines e o LiveData. A thread padrão do LiveData é a Main, por isso dá para controlar algumas coisas de UI nela mas, ao mesmo tempo, dá para usar operações main safe.

```kotlin
val plants: LiveData<List<Plant>> = liveData<List<Plant>> {
   val plantsLiveData = plantDao.getPlants()
   val customSortOrder = plantsListSortOrderCache.getOrAwait()
   emitSource(plantsLiveData.map {
       plantList -> plantList.applySort(customSortOrder) 
   })
}

fun getPlantsWithGrowZone(growZone: GrowZone) = liveData {
    val plantsGrowZoneLiveData = plantDao.getPlantsWithGrowZoneNumber(growZone.number)
    val customSortOrder = plantsListSortOrderCache.getOrAwait()
    emitSource(plantsGrowZoneLiveData.map { plantList -> 
        plantList.applySort(customSortOrder)
    })
}

class MyViewModel: ViewModel() {
        private val userId: LiveData<String> = MutableLiveData()
        val user = userId.switchMap { id ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(database.loadUserById(id))
            }
        }
    }
```

Algumas informações [deste artigo](https://android.jlelse.eu/coroutine-in-android-working-with-lifecycle-fc9c1a31e5f3) são tão importantes que não devem virar apenas uma nota de rodapé. Esse builder `liveData{}` recebe dois parâmetros opcionais de **CoroutineContext** e **timeOut** em milissegundos. O timeout é usado para esperar antes de cancelar e o coroutineContext deve ser evitado (não é apresentada uma explicação válida, mas chuto que seja porque ele pode afetar o ciclo de vida do LiveData). Algumas regras importantes:

1. O bloco ``liveData{}` só vai começar a ser executado quando o LiveData retornado estiver ativo, ou seja, quando é observado.
2. Se o LiveData ficar inativo quando o bloco estiver executando, ele vai ser cancelado depois do timeout, a não ser que o LiveData fique ativo antes de chegar no limite deste timeout. Qualquer valor emitido de um bloco cancelado vai ser ignorado. 
3. Após ser cancelado, se o LiveData ficar ativo de novo, o bloco vai ser executado novamente do começo.
4. Se o bloco é completado com sucesso ou é cancelado por outras razões que não o LiveData ficando inativo, ele não será executado novamente mesmo depois do LiveData passar pelo ciclo de ativo/ inativo.
5. Se qualquer uma das suspend functions dentro do bloco falhar, todo o bloco é cancelado e não reiniciado, o que acaba evitando leaks.



#### emit e emitSource

O método `emit()` seta um valor para o LiveData. 

Com o  `emitSource()` é possível não apenas emitir um único valor, mas vincular seu LiveData com outro e começar a emitir a partir dele. Dentro de um bloco de LiveData, você pode chamar um `emitSource()` para setar um LiveData como fonte para o LiveData retornado. Ele é similar ao `MediatorLiveData.addSource()`. Se o LiveData passado for alterado, então o LiveData retornado também vai mudar automaticamente.

> Cada chamada de `emit()` ou `emitSource()` vai remover a fonte adicionada anteriormente. Além disso, ambas possuem main safety, ou seja, inferem uma troca de thread, assim é possível chamar esses métodos em qualquer thread. Além disso, ambos podem ser chamados várias vezes em um bloco `liveData()`

```
var someData = liveData {
     val cachedData = dataRepository.getCachedData()
			// a Activtity que estiver observando  esse someData, vai receber  direto o  cachedData e vai  atualizar a tela
			emit(cachedData)

     val actualData = dataRepository.getData()
  		// o LiveData vai fazer o request para o network e vai substituir o cachedData para o actualData, que vai ativar o observer na Activity e atualizar as informações na tela
     emitSource(actualData)
 }
```

Este exemplo é simples, mas imagine que você tem uma cadeia de tarefas dependentes umas das outras que precisam ser executadas uma após a outra, cada uma sendo observada na Activity. O `emitSource` é bem útil para ser chamado várias vezes em um bloco LiveData.

```kotlin
// Um LiveData que recupera um objeto User baseado no userId e atualiza a cada 30 segundos
// enquanto for observado
val userId : LiveData<String> = ...
val user = userId.switchMap { id ->
    liveData {
      while(true) {
        // perceba que usar `while(true)` é OK porque o delay abaixo vai contribuir com o
        // cancelamento se o LiveData não for mais observado ativamente (pelo vínculo com Lifecycle)
        val data = api.fetch(id) // tratamento de erros ignorado para manter isso breve
        emit(data)
        delay(30_000)
      }
    }
}
```



# Cancelamento e exceções

Tão importante quanto saber mudar de threads, é saber não usar. O cancelamento é importante para evitar onerar mais do que o necessário e um bom tratamento de exceções pode ser a chave para a experiência do usuário. O conteúdo a seguir tem como base [o material de dois Google Advocates, Manuel Vivo e Florina Muntenescu](https://medium.com/androiddevelopers/coroutines-first-things-first-e6187bf3bb21).



## Cancelamento/ interrupção

Cancelamento de coroutine é [cooperativo](https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html). Isso significa que o código precisa checar pelo cancelamento explicitamente, o que acontece geralmente quando você chama as funções de kotlinx-coroutines. Por causa do bloco `withContext` só fazer chamadas do tipo blocking, ela não vai ser cancelada até que haja um retorno de `withContext`.

Para resolver isso, você pode chamar `yield` regularmente para dar a outras coroutines a chance de rodar e verificar o cancelamento. No código acima, é possível adicionar um `yield` entre o request do network e o query do database. Então, se o coroutine for cancelado enquanto estiver fazendo o request no network, não salvará no database.

Você também pode criar cancelamentos explícitos criando interfaces de low-level de coroutines.



## Exceptions

Exceções em funções suspensas funcionam como erros em funções normais. Se você lança um erro em uma função suspensa, ela vai ser jogada para quem chamou. Então mesmo que elas executem de um jeito diferente, você pode usar blocos `try/ catch` normais para lidar com eles. Isso é útil porque deixa você usar o suporte existente da linguagem para lidar com o erro ao invés de ter que tratar o erro a cada callback. 



# Suporte para Room e Retrofit



> Você não precisa usar `withContext()` para chamar funções suspensas **main-safe**. Por convenção, você deve garantir que as funções suspensas escritas no app são main-safe. Desta forma é seguro chamá-las de qualquer dispatcher, até `Dispatchers.Main`. Este é o caso depois do suporte para Room e Retrofit. Essas funções já são main-safe, não precisa usar novamente o `mainContext()`.



## Room

```kotlin
// add the suspend modifier to the existing insertTitle

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertTitle(title: Title)
```

Quando você transforma o método em suspend, Room vai fazer com que a query seja main-safe e execute em uma background thread (IO) automaticamente. Contudo, isso significa que você só pode chamar essa query de dentro de um coroutine.



### Flow com Room

```kotlin
//DAO
@Query("SELECT * from plants ORDER BY name")
fun getPlantsFlow(): Flow<List<Plant>>

@Query("SELECT * from plants WHERE growZoneNumber = :growZoneNumber ORDER BY name")
fun getPlantsWithGrowZoneNumberFlow(growZoneNumber: Int): Flow<List<Plant>>
```

Quando é especificado que o retorno é do tipo Flow, o Room executa o query com as seguintes características:

- Main-safety
- Observas as mudanças e emite novos valores para o Flow
- Sequência async - Flow emite todo o resultado do query a cada mudança, e não vai introduzir nenhum buffer. Se retorna um `Flow<List<T>>`, o Flow vai emitir um `List<T>` que contém todas as linhas do resultado do query. Vai executar como uma sequência - emitindo um resultado do query por vez e suspendendo até que seja solicitado um novo.
- Cancelável - Quando o escopo que está coletando esse flow for cancelado, o Room cancela a observação desta query.





## Retrofit

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



# Structured concurrency

https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/

[Simultaneidade estruturada](https://medium.com/@elizarov/structured-concurrency-722d765aa952)



# Links

- [Utilizando coroutines no Android](https://medium.com/android-dev-br/utilizando-kotlin-coroutines-no-android-c73fcda71e27)
- [Codelabs de coroutines](https://codelabs.developers.google.com/codelabs/kotlin-coroutines/#0)
- [Codelabs de coroutines avançado com Kotlin Flow e LiveData](https://codelabs.developers.google.com/codelabs/advanced-kotlin-coroutines/index.html#0)
- [Melhorar o desempenho do app com coroutines](https://developer.android.com/kotlin/coroutines)
- [Exemplos de testes Codelabs](https://github.com/googlecodelabs/kotlin-coroutines/tree/master/coroutines-codelab/finished_code/src/test/java/com/example/android/kotlincoroutines/main)
- [Motivos para evitar o GlobalScope](https://medium.com/@elizarov/the-reason-to-avoid-globalscope-835337445abc)
- [Coroutines com Lifecycle API e LiveData](https://android.jlelse.eu/coroutine-in-android-working-with-lifecycle-fc9c1a31e5f3)
- [Palestra do Nelson Glauber sobre coroutines - contém um resumo de Exceptions](https://github.com/ninalofrese/studiesinkotlin/blob/master/coroutines-nglauber.pdf)
- [Cancelamento e exception em coroutines](https://medium.com/androiddevelopers/coroutines-first-things-first-e6187bf3bb21)
- [Trocando RxJava por Coroutines](https://proandroiddev.com/i-exchanged-rxjava-for-coroutines-in-my-android-application-why-you-probably-should-do-the-same-5526dfb38d0e)
- [Documentação sobre context e dispatchers](https://kotlinlang.org/docs/reference/coroutines/coroutine-context-and-dispatchers.html)
- [Desmistificando CoroutineContext](https://proandroiddev.com/demystifying-coroutinecontext-1ce5b68407ad)
- [Exemplos coroutines considerando memory safe por conta de um blocking code](https://github.com/ninalofrese/android-multithreading-masterclass/tree/master/app/src/main/java/com/techyourchance/multithreading/demonstrations/designcoroutines)
- [Exemplo com fatorial](https://github.com/ninalofrese/android-multithreading-masterclass/tree/master/app/src/main/java/com/techyourchance/multithreading/solutions/exercise10)
- [Coroutines tips and tricks](https://proandroiddev.com/coroutines-snags-6bf6fb53a3d1)
- [Como entender Kotlin coroutines](https://proandroiddev.com/how-to-make-sense-of-kotlin-coroutines-b666c7151b93)
- [Coroutines concurrency](https://kotlinexpertise.com/kotlin-coroutines-concurrency/)