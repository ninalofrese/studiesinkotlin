# Injeção de dependência

> Dependências são objetos que uma classe precisa para realizar os comportamentos esperados, portanto, se uma classe acessa o banco de dados e usa um DAO para isso, o DAO é uma dependência da classe.

Uma das formas de se fazer inversão de dependência é fazer a injeção de dependência, um pattern que possibilita garantir um baixo acoplamento em uma cadeia de classes. A grosso modo, injetar uma dependência é passar uma classe que será utilizada para uma classe que irá consumi-la. É uma técnica que delega a responsabilidade de inicializar dependências para o software. Por exemplo, ao invés de instanciarmos as dependências em algum momento do código, o próprio framework de injeção de dependência realiza estes passos para a gente. O padrão de injeção de dependência trabalha baseado em abstrações, sejam elas classes abstratas ou interfaces. 

> Se pudéssemos citar um “lema”, este seria: programe para uma interface e nunca para uma implementação. E este “lema” realmente faz diferença quando queremos diminuir o acoplamento entre as classes do nosso modelo.

Podemos trabalhar com a injeção de dependência de três formas:

- Injeção por construtor (constructor injection)
- Injeção por propriedade, ou getters e setters no caso do Java (setter injection)
- Injeção por interface (interface injection)

Nem sempre a injeção de dependência é a inversão de dependência, mas ela é uma ferramenta para isso.

O grande benefício é delegar a responsabilidade de inicialização de dependências, permitindo que membros do projeto apenas peçam o que precisam e a instância é fornecida automaticamente de acordo com o escopo necessário, como por exemplo, um *Singleton* ou *Factory* (instância sempre nova).



## Koin

O Koin é uma lib externa que precisa ser adicionada como uma dependência. Documentação: https://insert-koin.io



### Módulos

A base de configuração do Koin são os módulos, que são as entidades que mantém as instruções de como as dependências devem ser inicializadas. Essa é a implementação básica:

```kotlin
val registerDataModule = module {
    factory<RegisterRepository> { RegisterRepositoryImplementation(get()) }
}

//ou

val registerRemoteModule = module {
    //neste caso, é um factory para a classe de RetrofitService
    factory {
        ServiceBuilder().invoke<RegisterService>(
                BuildConfig.BASE_URL,
                get(named("user_token"))
        )
    }
    factory<RegisterRemoteRepository> { RegisterRemoteImplementation(get()) }
}
```

Em projetos mais complexos, pode-se ter mais de um módulo, e depois eles podem ser declarados como uma lista:

```kotlin
val RegisterModule = module {
    loadKoinModules(
            listOf(
                    registerDomainModule,
                    registerDataModule,
                    registerRemoteModule,
                    registerViewModule
            )
    )
}
```



### Factory

O **factory** indica ao Koin que cada vez que injetar uma instância com a referência (que está entre os {}), ele vai criar uma instância nova. A instância só é criada na hora que "chama", quando for necessário. Quando a classe implementa uma interface, precisa deixar explícito que o tipo de factory é o da interface.

Para passar os parâmetros do construtor, o Koin pega o contexto com o **get()**. Vale lembrar que toda referência via `get()` tem que ser de conhecimento do Koin, ou seja, definida via módulo para que ele consiga criar a instância esperada.

```kotlin
//factory<interface> { implementacao()}
factory<RegisterRepository> { RegisterRepositoryImplementation(get()) }
```



### Single

São instâncias únicas.

```kotlin
single { MyRepository() }
```



### Inicialização do Koin

A referência de context que o Koin utiliza por meio do `get()` precisa vir de algum lugar. E é na inicialização do Koin que é indicado isso. Por isso, o Koin é inicializado dentro de uma entidade que tenha referência ao Context do Android. Isso pode ser feito dentro do `onCreate()` da Activity, mas não é muito indicado porque, se ela for destruída e criada novamente, perde as referências pelas quais o Koin fez a injeção. Por isso é melhor inicializar o Koin dentro da implementação de uma Application, como no exemplo abaixo.

```kotlin
class ShipperApplication : Application() {

    override fun onCreate() {
    	startKoin {
            androidContext(this@ShipperApplication)
            androidLogger()
            LoginModule
            VehicleTypeModule
            ShipmentModule
            RecoveryModule
            RegisterModule
            loadKoinModules(SessionModule)
            loadKoinModules(listOf(landingModule, supportModule,RemoteConfigModule))
        }
    }
```



### Injeção de dependências por meio de delegated property

Assim como na inicialização via lazy, ao usar o `by inject()`, a property é mantida imutável

```
private val adapter: ProductsListAdapter by inject()
```



### Injeção de dependências de objetos com mais complexidade

Com o exemplo de configurar a instância do DAO, a injeção não é tão simples porque, considerando a regra na qual ele é instanciado, precisa de uma referência do `AppDatabase`

```kotlin
// Forma simplificada de fazer o RetrofitService com as abstrações que temos no app do shipper
factory {
        ServiceBuilder().invoke<RegisterService>(
                BuildConfig.BASE_URL,
                get(named("user_token"))
        )
    }

// Exemplo de instanciação do DAO
val techStoreModule = module {
    factory { ProductsListAdapter(context = get()) }
    //referência à instância do AppDatabase com o single, que cria uma instância única
    single { Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "techstore-database")
            .build()}
    //O Koin fornece uma instância do AppDatase e, a partir dela, cria a instância do DAO
    single { get<AppDatabase>().productDao() }
}
```



# Links

[Injeção de dependência no Kotlin com Koin - inclui testes](https://medium.com/collabcode/injeção-de-dependência-no-kotlin-com-koin-4d093f80cb63)