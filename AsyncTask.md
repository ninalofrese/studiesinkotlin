# AsyncTask

AsyncTask é um framework que já foi muito utilizado no Android, mas que hoje é melhor ser evitado. Mas é importante entender o porquê.



`new AsyncTask<Void, Void, Void>()` possui alguns métodos interessantes:

- `void onPreExecute()` é chamado na UI Thread para preparar para a computation
- `Void doInBackground(Void... voids)` é em background thread
- `void onPostExecute(Void)`é sempre executado na UI thread

A AsyncTask pode ser executada com dois métodos no final: `execute()` que executa sequencialmente em uma single background thread, ou com `executeOnExecutor()`, que permite multithreading.



```java
// 1o void é um parâmetro para executar em background | doInBackground()
// 2o void é para publicar o status da computation
// 3o void é o tipo de retorno da execução em background | doInBackground()
// que também vai como parâmetro para o onPostExecute()
new AsyncTask<Void, Void, Void>() {

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
  }

  @Override
  protected Void doInBackground(Void... voids) {
    synchronized (LOCK) {
      while (mNumOfFinishedConsumers < NUM_OF_MESSAGES) {
        try {
          LOCK.wait();
        } catch (InterruptedException e) {
          return null;
        }
      }
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    notifySuccess();
  }
}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
```



Vantagens:

- Nenhuma?



Desvantagens:

- Baseado em herança: tem que sair estendendo de uma outra classe e implementar os métodos
- API complexa e fragmentada: não dá nem para executar em uma background thread
- ThreadPoolExecutor mal configurado
- Documentação ruim e lints contém muitas informações erradas e que promovem práticas ruins, como indicar que você não gostaria de rodar seu código em concurrency, mas sequencial (WTF)



Existe um mito de que se você usar AsyncTasks está mais suscetível a memory leaks, mas não é verdade. O risco é absolutamente o mesmo do que se usar qualquer outro framework.

A ideia dessa API era simplicar a concurrency, mas no fim das contas teve tanto bug e tantos problemas, que foi reescrita para usar execução sequencial e, no fim das contas, simultaneidade é algo tão complexo que não tem como só simplificar.

