# ThreadPoster

Framework criado por [Vasiliy Zukanov](https://www.udemy.com/user/vasiliy-zukanov/), por isso muitas informações apresentadas possuem um viés. O ThreadPoster está no [Github](https://github.com/techyourchance/thread-poster), mas pode ser incluído no projeto por essa dependência:

```groovy
implementation 'com.techyourchance.threadposter:threadposter:0.8.3'
```

É um framework muito simples que abstrai o ThreadPool e o Handler.



Vantagens:

- Simples de usar
- Nomeclatura explícita
- Não precisa chamar `.start()` em novas instâncias criadas de Thread
- Permite unit test das multithreads



Desvantagens:

- Não é tão maduro quanto outros frameworks
- É uma API muito simples, que pode limitar em alguns casos
- A abordagem aplicada para unit test não é muito padronizada

