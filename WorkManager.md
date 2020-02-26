# WorkManager

Faz tarefas quando o app está no background. É uma API leve que permite agendar tarefas quando o app está no background ou não está rodando.

Você indica constraints para que ele realize tarefas quando o aparelho está carregando, no WiFi, sem uso, etc.

O uso do WorkManager é importante inclusive para deixar mais eficiente o consumo da bateria.

![work-manager](T:\Meu Drive\Estudos\work-manager.png)

## Bateria eficiente

- Otimizada (CPU, Disco)
- Pouco uso da rede
- Rodar raramente

## Pre-fetching

- Carrega o que o usuário precisará em breve
- Em background
- Geralmente feito à noite
- WorkManager agenda eficientemente

## WorkManager na prática

### Criar uma background task

Para criar uma task que rode em background, você precisa criar uma classe que estenda o `Worker`. No método `doWork()` especifique o que deve ser feito e retorne o resultado (tanto em caso de sucesso, como de erro ou retry).

```kotlin
class SyncUserDataWorker(
        context: Context,
        workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        val success = syncUserData() //long running task
        return if (success) Result.success() else Result.failure()
    }
}
```

O método `doWork()` é executado de forma **síncrona** no encadeamento em segundo plano fornecido pelo `WorkManager`, então, na maioria dos casos, não precisa se preocupar com threads aqui.

### Enfileirando o trabalho

Para rodar o trabalho, primeiro, você precisa criar um `WorkRequest`. Para `WorkRequests` únicos, use o `OneTimeWorkRequestBuilder`. Ele permite que crie o request do trabalho que será executado uma vez pelo `WorkManager`.

```kotlin
val syncDataRequest = OneTimeWorkRequestBuilder<SyncUserDataWorker>().build()
```

Assim que o `WorkRequest` é criado, ele pode ser agendado pela chamada do método `enqueue()` do WorkManager.

```kotlin
WorkManager.getInstance().enqueue(syncDataRequest)
```

Usando o código acima ele agendará o work request para ser executado imediatamente. Mas o WorkManager tem muito mais potencial que isso. Você pode agendar a task para ser executada quando requisitos específicos forem atingidos (ex: o telefone está carregando), criando um trabalho recorrente usando `PeriodicWorkRequestBuilder` ou encadeando múltiplos `WorkRequests`.

### Passar e recuperar dados do worker

Outra função incrível é que o WorkManager permite que você passe um dado de input do worker e retorne um outro dado output dele. Por exemplo, você pode criar um worker que recebe um username como input, processar esse dado e retornar o usuário identificado como resultado.

```kotlin
class SyncUserDataWorker(
        context: Context,
        workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        val userName = inputData.getString(KEY_USER_NAME)
        val response = syncUserData(userName) //long running task

        val outputData = workDataOf(KEY_USER_ID to response.userId)
        return Result.success(outputData)
    }
}
```

O dado de input pode ser criado usando o método `workDataOf()`, que é parte do work-runtime-ktx, mas também pode gerar usando o `Data.Builder` diretamente. O dado criado é passado paa o `OneTimeWorkRequestBuilder` usando o método `setInputData(Data)`.

```kotlin
// workDataOf (part of KTX) converts a list of pairs to a [Data] object.
val inputData = workDataOf(KEY_USER_NAME to userName)

//without KTX
val inputDataWithoutKtx = Data.Builder()
         .putString(KEY_USER_NAME, userName)
         .build()

//pass the input data to worker
val syncDataRequest = OneTimeWorkRequestBuilder<SyncUserDataWorker>()
         .setInputData(inputData)
         .build()
```



## Work Manager com RXJava

Se você quer usar observáveis dentro do worker, não tem problema. Para que haja uma interação entre WorkManager e RXJava, a dependência `work-rxjava2` foi criada pelo time de Android. Depois de incluir [androidx.work:work-rxjava2:$work_version](https://developer.android.com/jetpack/androidx/releases/work?hl=pl#declaring_dependencies) no arquivo gradle, você pode criar o Worker que estende de RXWorker. É similar ao Worker normal, só que ao invés de ter `doWork()` que retorna um `Result`, tem um método `createWork()`, que retorna um `Single<Result>`, então dá para colocar funções e operadores reativos dentro dele.

```kotlin
class RxSyncUserDataWorker(
        context: Context,
        workerParameters: WorkerParameters
) : RxWorker(context, workerParameters) {
		
  	// Método chamado na MainThread, mas o retorno Single é subscribed no BackgroundThread
    override fun createWork(): Single<Result> {
        return syncUserData()
                .map { response ->
                    val outputData = workDataOf(KEY_USER_ID to response.userId)
                    Result.success(outputData)
                }
    }
}
```

- Não precisa se preocupar sobre disposing o Observer, já que o RxWorker fará isso automaticamente quando o trabalho terminar.
- Retornar tanto Single com o valor `Result.failure()` e um Single com um erro vai levar ao worker entrar no estado de falha (failed state)

### Observando o status do trabalho e recuperando dados de output

Depois de enfileirar o `WorkRequest`, o WorkManager permite acompanhar o seu status. Essa informação é armazenada em um objeto `WorkInfo` que guarda o id do trabalho, tags, o estado atual e o valor de resultado se ele já retornou. Dá pra fazer isso de 3 maneiras.

#### 1. Recuperar o `WorkInfo` para um `WorkRequest` específico pelo ID usando os seguintes métodos

`WorkManager.getWorkInfoByIdLiveData(UUID)` - permite observar o `WorkInfo` usando `LiveData` e reagindo às mudanças de estado

```kotlin
WorkManager.getInstance().getWorkInfoByIdLiveData(syncDataRequest.id)
        .observe(lifecycleOwner, Observer { workInfo ->
            if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                val userId = workInfo.outputData.getString(KEY_USER_ID)
                displayMessage("Sync work finished! $userId")
        }
})
```

`WorkManager.getWorkInfoById(UUID)` - retorna `ListenableFuture<WorkInfo>` - um Future que permite adicionar o listener que será notificado quando o trabalho com o ID específico for completado.

```kotlin
WorkManager.getInstance().getWorkInfoById(syncDataRequest.id)
        .addListener(Runnable {
            displayMessage("Sync work finished!")
        }, executor)
```

#### 2. Recuperar os objetos `WorkInfo` com a tag específica. A tag é passada durante a criação do WorkRequest

```kotlin
val syncDataRequest = OneTimeWorkRequestBuilder<SyncUserDataWorker>()
        .addTag(TAG_SYNC_WORK)
        .build()
```

Então é possível observar os estados da lista de WorkRequests onde foi criada com a tag atribuída.

	- usando o `LiveData<List<WorkInfo>>` recuperada por `WorkManager.getWorkInfosByTagLiveData(String)`
	- ou usando o `ListenableFuture<List<WorkInfo>>` retornado por `WorkManager.getWorkInfosByTag(String)`

#### 3. Para um unique work também tem métodos similares

- `WorkManager.getWorkInfosForUniqueWorkLiveData(String)` que permite observar os estados dos  `WorkRequest`s com `LiveData>`.

- ou `WorkManager.getWorkInfosForUniqueWork(String)` que retorna um `ListenableFuture>`.

Um unique work é criado da mesma maneira que os trabalhos normais, mas é enfileirado com o ID específico e a política de trabalho existente.

***

Material de referência:

- https://proandroiddev.com/how-to-use-workmanager-with-rxjava-b5936f68e024
- https://medium.com/swlh/workmanager-basics-how-to-use-workmanager-with-rxjava2-kotlin-coroutines-c2a317197038