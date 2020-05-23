# Kotlin

É uma linguagem que mescla **POO** e **funcional**. É segura porque evita NPE (_Null Pointer Exception_).

Algumas diferenças de Java para Kotlin:

- `extends` e `implements` se tornam `: 	`
- métodos são expressos com `fun`
- variáveis são expressas com `val` ou `var`. `val` é uma variável de somente leitura (imutável), portanto só pode ter um valor atribuído uma só vez. `var` são variáveis  mutáveis, que podem ter valores atribuídos mais de uma vez. É muito comum se usar o `val` no dia-a-dia.
- Construtores vêm no parâmetro da classe
- `lateinit var` é utilizado quando se quer inicializar uma variável posteriormente
- `?` significa que pode receber **null** e, se for nulo, ele não vai acessar a referência.
- `findViewById` cai em desuso. No Kotlin, a lógica reconhece direto o elemento do XML pelo seu id, o que é chamado de View Binding.
- Tenta evitar variáveis estáticas sempre que possível. O Kotlin prefere aninhar essas variáveis em instâncias ou companion objects.
- Cast é feito usando a palavra `as` e a checagem de tipo é feita usando a palavra `is`
- `Any` é um tipo usado para qualquer tipo de objeto/ `Nothing` é usado para nenhum tipo de objeto
- O receiver é o tipo sendo estendido na extension function ou no nome da classe. Qualquer bloco de código no Kotlin pode ter um ou múltiplos **receivers** , fazendo com que funções e propriedades do receiver fiquem disponíveis naquele bloco sem precisar especificar. Exemplo: `A.(B) -> C` representa uma função que pode ser chamada em um objeto receptor de A com um parâmetro de B e retornar um valor de C.

## Null Safety

O Kotlin procura evitar os erros de NPE tão costumeiros do Java, então uma variável por natureza não pode ser nula no Kotlin. Mas, se precisa lidar com uma variável nula, você pode usar o **safe call operator ?.** ou o **not-null assertion operator !!** . 

Usar o **?** significa que a variável poderá ser nula e, caso ela seja, sua referência não será acessada. Já o **!!** converte qualquer valor para um valor não-nulo, mas caso ele seja nulo, será lançada a exceção NPE. 

| a: String? | a.length           | a?.length | a!!.length           |
| ---------- | ------------------ | --------- | -------------------- |
| "cat"      | Compile time error | 3         | 3                    |
| null       | Compile time error | null      | NullPointerException |



### ?. Safe call operator

Retorna o seu valor se não for nulo, e se for nulo, retorna null. É muito útil em encadeamentos, pois se um objeto é nulo, ele não executa o restante à direita, é por isso que funciona como a verificação de nulo do Java, mas de forma muito mais simples.

```kotlin
val a = "Kotlin"
val b: String? = null
println(b?.length) // Prints null
println(a?.length) // Unnecessary safe call, prints 6

val listWithNulls: List<String?> = listOf("Kotlin", null)
for (item in listWithNulls) {
    item?.let { println(it) } // prints Kotlin and ignores null
}
```



### !! Not-null Assertion Operator

Esse operador força qualquer valor para um valor não-nulo e lança uma NPE se o valor for nulo. É usado com muita cautela, porque você precisa ter certeza que o valor que está convertendo não é nulo.

```kotlin
fun strLen(value: String?) = value!!.length
strLen(null)
// NullPointerException
```

### ?: Operador Elvis

Quando se declara uma variável, ela não pode ser nula, em Kotlin. Já foi dito que quando quer que seja nula, usa-se o `?` e o operador Elvis usa verifica justamente isso. Com o operador `?:`,  ele diz que a expressão só irá considerar seu próprio valor se não for nula, e atribuirá outro caso for nula.

Um exemplo:

```kotlin
val test: Int? = null
//caso test for nulo, ganhará o valor de 100; se não for nulo manterá seu próprio valor
val op: Int = test ?: 100
```

### as? Safe Casts

Casts normais podem acabar resultando em _ClassCastException_ se o objeto não for do tipo especificado (geralmente por conta do null). Então uma opção é fazer safe casts que retornam null caso a tentativa não tiver sucesso.

```kotlin
val x: Int = 1

x as String? // Causes ClassCastException, cannot assign Int to String?

x as? String // Returns null, since x is not a String type.
```



## Expressões

Expressões retornam valores. Exemplo:

```kotlin
override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
```

## Funções

As funções diferem muito pouco do método usual do Java, porém ele permite que se atribua um valor padrão (ou inicialize) direto ao passar os argumentos. É importante que os parâmetros que têm valores inicializados sejam deixados por último.

```kotlin
fun canAddFish(tankSize: Double, currentFish: List<Int>, fishSize: Int = 2, hasDecorations: Boolean = true): Boolean {
  //o sum é um recurso interessante que soma os valores de todos os itens da lista, sem precisar de loop
    return (tankSize * if (hasDecorations) 0.8 else 1.0) >= (currentFish.sum() + fishSize)
}
```



### Extension functions

As extension functions permitem estender uma classe existente com novas funcionalidades. Antes do ponto especificamos da qual classe se estende e colocamos o nome da função. O `this`, neste caso referenciado dentro da função de extensão, está ligado à instância na qual é chamado. Funções de extensão são definidas fora da classe da qual estendem e, portanto, não têm acesso às variáveis privadas. Além disso, o uso mais comum é para estender classes que não foram criadas por nós, como de API ou do próprio Kotlin/ Android, e a questão da visibilidade torna mais raro a utilidade de funções de extensão para classes do projeto.

```kotlin
//in new file _LayoutInflater.kt
val Context.layoutInflater get() = LayoutInflater.from(this)

//example inside adapter
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val view = context.layoutInflater.inflate(R.layout.item_note, parent, false)
        return NotesViewHolder(view)
    }

//Outro exemplo
fun String.hasSpaces() : Boolean{
    val found = this.find { it == ' ' }
    return found != null
}
//ou
fun String.hasSpaces() = find{ it == ' ' } != null

fun extensionsExample(){
    "Does it have spaces?".hasSpaces() //true
}
```

Propriedades também podem ser estendidas.

```kotlin
var AquariumPlant.isGreen: Boolean
get() = color = "Green"
```

> Dentro de uma função de extensão é chamado o **receiver object** como argumento. A função é determinada como uma extensão de String, então dentro da função String é chamado o **receiver type**. O termo não é receiver class porque extension functions não estendem de classes, mas sim de TIPOS.

### Funções de ordem superior (high-order function)

Essas funções são utilizadas em todo o lugar no Kotlin. São funções que têm funções como argumento e vivem abrigando um lambda.

```kotlin
//um exemplo simples é o with
fun fishExamples(){
  val fish = Fish("splashy")
  
  with (fish.name){
    capitalize()
  }
  
  //aplicando o myWith
  myWith(fish.name){
    capitalize()
  }
}

//criação "manual" do with, que passa como argumento o objeto que queremos usar e a função que queremos executar no objeto
// block, dentro de myWith, é uma função de extensão em um objeto String
fun myWith(name: String, block: String.() -> Unit){
  name.block()
}
```

Cada função de ordem superior cria um objeto para esta função, aloca esse objeto na memória, o que insere sobrecarga no tempo de execução.



### Inline

Com a construção acima, toda vez que chamar `myWith()`, o Kotlin criará um novo objeto lambda, o que aumenta o consumo de memória e CPU. A inline function se comporta como um *promise*, então sempre que a função inline for chamada, o compilador transformará o código-fonte substituindo o lambda pelas instruções dentro do lambda.

```kotlin
//É uma "promessa" de que sempre que myWith for chamado ele transformará o código fonte para criar um inline da função

inline fun myWith(name: String, block: String.() -> Unit){
  name.block()
}
```

A vantagem de usar inline functions é que elas reduzem a sobrecarga do tempo de execução drasticamente, já que a alocação adicional na memória heap é evitada. É importante notar que fazer inline de métodos que não possuem outra função como parâmetro não é vantajoso em termos de performance. Outro ponto é que as inline functions aumentam bastante o código gerado pelo compilador, por isso é melhor evitar usar em funções grandes. 

> Em inline functions, não é possível usar uma função do parâmetro como parâmetro de outro método, o que gera o erro `Illegal usage of inline-parameter`. Caso a função tenha mais de um parâmetro, pode-se usar a palavra `noinline` para ignorar o efeito do inline apenas para esta função. Mas se a função tem apenas 1 função como parâmetro, melhor nem considerar usar o inline.

Alguns casos de uso legais são:

- Representar tipos de forma mais explícita

```kotlin
inline class Password(val value: String)
inline class UserName(val value: String)

fun auth(userName: UserName, password: Password) { println("authenticating $userName.")}

fun main() {
    auth(UserName("user1"), Password("12345"))
    //does not compile due to type mismatch
    auth(Password("12345"), UserName("user1"))
}
```

- Tratamento de estado sem alocar espaço adicional

```kotlin
//Este caso mostra como monitorar a String original, mas sem alocar espaço extra

inline class ParsableNumber(val original: String) {
    val parsed: BigDecimal
        get() = original.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
}

fun getParsableNumber(number: String): ParsableNumber {
    return ParsableNumber(number)
}

fun main() {
    val parsableNumber = getParsableNumber("100.12212")
    println(parsableNumber.parsed)
    println(parsableNumber.original)
}

//getParsableNumber() retorna uma instância da inline class que fornece duas propriedades, original e parsed
```

- Reduzir o escopo de uma extension function

```kotlin
//ao invés de estar disponível para qualquer String, asJson() será executado em uma String wrapped em JsonString.

inline class JsonString(val value: String)
inline fun <reified T> JsonString.asJson() = jacksonObjectMapper().readValue<T>(this.value)
```



### Operator

A palavra `operator` indica um overloading de uma classe, marcando aquele método como o overloading de uma operação ou implementando uma nova convenção.

https://kotlinlang.org/docs/reference/operator-overloading.html



## Classes de dados (data class)

Classes de dados geram de forma simples construtores e suportam hashCodes e equals. A única coisa que não gera automaticamente é serialização. Com ela, os modelos ficam mais simples e concisos.

```kotlin
//all the code in model Note.kt
data class Note(
        var id: Int = -1,
        var text: String? = null,
        var isPinned: Boolean = false,
        var createdAt: Date = Date(),
        var updatedAt: Date? = null
)
```

## String interpolation

Permite adicionar expressões dentro da string, com o `$` indicando uma variável

```kotlin
val SQL_CREATE_ENTRIES = "CREATE TABLE $_TABLE_NAME ($_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, $TEXT TEXT, $IS_PINNED INTEGER, $CREATED_AT INTEGER, $UPDATED_AT INTEGER)"
```

## THIS vs. IT

### THIS - Referência ao objeto atual

Para indicar o receptor atual, usamos a expressão **this**.

- Em um membro de uma classe, this refere-se ao objeto atual daquela classe
- Em uma extension function ou uma função literal com receptor, o this denota o parâmetro receiver que é passado à esquerda do ponto. `this.apply()` 
- Se this não tiver qualificadores, se refere ao escopo mais interno. Neste caso, para se referir a escopos mais gerais (abertos), é necessário indicar um this qualificado. `this@MainActivity`

### IT - Nome implícito de um único parâmetro

- É um nome implícito de um único parâmetro de uma expressão lambda.

## Scopes

https://kotlinlang.org/docs/reference/scope-functions.html

https://medium.com/@fatihcoskun/kotlin-scoping-functions-apply-vs-with-let-also-run-816e4efb75f5

https://github.com/MobileTipsters/android-daily-tips#131-understanding-let-apply-run-also-with

| Function | Object reference | Return value   | Is extension function                                     |
| :------- | :--------------- | :------------- | :-------------------------------------------------------- |
| `let`    | `it`             | Lambda result  | Yes                                                       |
| `run`    | `this`           | Lambda result  | Yes                                                       |
| `run`    | -                | Lambda result  | No: called without the context object (`this` é receiver) |
| `with`   | `this`           | Lambda result  | No: takes the context object as an argument.              |
| `apply`  | `this`           | Context object | Yes                                                       |
| `also`   | `it`             | Context object | Yes                                                       |

Situações para escolher cada scope:

 -  Executar um lambda em um objeto não-nulo: **let**

 -  Introduzir uma expressão como variável em um escopo local: **let**

 -  Configurar um objeto: **apply**

 -  Configurar um objeto e computar o resultado: **run**

 -  Executando instruções em que uma expressão é necessária: não-extendido **run**

 -  Efeitos adicionais: **also**

 -  Agrupar as chamadas de funções em um objeto: **with**

    

![scopes-fluxogram](scopes-fluxogram.png)

### Apply (retorna o objeto - THIS) - "aplique as seguintes orientações ao objeto"

É uma extensão que funciona com todos os tipos de dados. Diferentemente de run, que retorna o resultado da execução do lambda, o apply retorna o objeto no qual ele é aplicado. Exemplo: `fish.apply{}` retorna o objeto fish. O apply é muito usado para chamar funções no objeto recentemente criado.

Também é usado como um truque que muda o valor de `this` dentro do lambda. O apontador para o this é diferente. É muito útil para inicializar objetos.

- Pode ser usada em return statements de funções que retornem o objeto de contexto
- Como o receiver é o valor de retorno, é fácil usar o apply em encadeamentos para casos mais complexos.
- Usado para blocos de código que não retornam um valor e principalmente opere nos membros do objeto receptor. O principal uso é **configurar o objeto**

```kotlin
val fish2 = Fish(name = "splashy").apply{name = "Sharky"}
println(fish2.name) //Sharky
```

> A diferença é que **run** retornará o resultado da execução do lambda, enquanto **apply** retornará o objeto após a execução do lambda.

### Also (retorna o objeto - IT) - "e também fazer o seguinte"

- É bom para realizar ações que tenham o objeto de contexto como argumento. Use also para ações adicionais que não alterem o objeto, como imprimir ou criar logs.
- Permite encadear também, porque retorna o objeto
- Geralmente, remover as chamadas de also não devem quebrar a lógica.

```kotlin
//antes de atribuir uma Person, ele vai imprimir
val person: Person = getPerson().also {
    print(it.name)
    print(it.age)
}
```

### Let  (retorna o lambda result - IT)

Retorna uma cópia do objeto alterado, e é muito útil para encadear manipulações.

- Usado para criar escopos e para lidar com nullables
- Pode ser usada para invocar uma ou mais funções nos resultados dos encadeamentos
- É usado com frequência para executar um código só com valores não-nulos (usando o operador ?)
- Introduz variáveis locais com escopo limitado para melhorar a leitura do código

```kotlin
fish.let{it.name.capitalize()}
		.let{it + " fish"}
		.let{it.length}
		.let{it + 31}
//println - 42
```

### With  (retorna o lambda result - THIS) - "com este objeto, fazer o seguinte"

- Usado para várias chamadas no mesmo objeto
- Usado para chamar funções no objeto de contexto sem prover o resultado lambda
- Serve para introduzir um objeto ajudante cujas propriedades ou funções serão usadas para calcular um valor

```kotlin
//retorna o que está no bloco
val person: Person = getPerson()
with(person) {
    print(name)
    print(age)
}
```

### Run (retorna o lambda result  -  THIS - extensão opcional)

Funciona com todos os tipos de dados. Ela usa um lambda como argumento e retorna o resultado da execução do lambda. Exemplo: `fish.run{name}` retorna `name = splashy`

- Faz o mesmo que WITH, mas invoca como LET - como uma função de extensão do objeto de contexto
- É útil quando o lambda contém tanto a inicialização do objeto quanto o cálculo do valor de retorno
- Além de chamar o RUN em um objeto receptor, também pode ser uma função que não estenda. Isso permite que o run execute um bloco em vários statements onde uma expressão é necessária.



## Funções especiais

### Controle de fluxo when

São como switch-case, a melhor escolha quando há mais de 2 opções.

```kotlin
when (c){
	"Soma" -> 
		//executa um codigo de soma
    return a + b;
	
  "Subtração" -> 
    return a - b;
  
  else -> {
    println("Operação incorreta")
    return 0;
  } 
}
```

### Intervalo/  range

Há como testar intervalos de uma forma mais prática que no Java, usando  a palavra reservada `in`

```kotlin
val fishName = "Nemo"
when (fishName.length){
    0 -> println("Error")
    in 3..12 -> println("Good fish name")
    else -> "Ok fish name"
}
```

### vararg/ spread operator

O vararg permite que se passe um número variável de argumentos sem precisar criar um array. Ela só pode ser usada uma vez como parâmetro de uma função e, quando for receber o parâmetro, ele precisa estar sinalizado com o **spread operator**, representado por um asterisco *.

```kotlin
//vararg permite um número variável de argumentos sem precisar criar um array
@Insert(onConflict = OnConflictStrategy.REPLACE)
fun insertAll(vararg videos: DatabaseVideo)

//como o insertAll usa vararg, ele precisa do spread operator (*), que permite passar
// uma array para uma função que espera varargs
database.videoDao.insertAll(*playlist.asDatabaseModel())
```

## Listas

```kotlin
val array = Array(7){1000.0.pow(it)}
val sizes = arrayOf("byte", "kilobyte", "megabyte", "gigabyte", "terabyte", "petabyte", "exabyte")
for((i, value) in array.withIndex()){
    println("1 ${sizes[i]} = ${value.toLong()} bytes")
}
//1 byte = 1 bytes1 kilobyte = 1000 bytes1 megabyte = 1000000 bytes1 gigabyte = 1000000000 bytes1 terabyte = 1000000000000 bytes1 petabyte = 1000000000000000 bytes1 exabyte = 1000000000000000000 bytes

var list3: MutableList<Int> = mutableListOf()
for(i in 0..100 step 7) list3.add(i)
print(list3)
//[0, 7, 14, 21, 28, 35, 42, 49, 56, 63, 70, 77, 84, 91, 98]

for(i in 0..100 step 7) println(i.toString() + " - ")
//0 - 7 - 14 - 21 - 28 - 35 - 42 - 49 - 56 - 63 - 70 - 77 - 84 - 91 - 98 - 
```

https://kotlinlang.org/docs/reference/collections-overview.html

![Collections in Kotlin](collections-diagram.png)

Alguns exemplos de listas:

```kotlin
var fortune: String
    for (i in 1..10) {
        fortune = getFortuneCookie(getBirthday())
        println("\nYour fortune is: $fortune")
        if (fortune.contains("Take it easy")) break
    }  

var fortune: String = ""
    repeat(10) {
        fortune = getFortuneCookie(getBirthday())
        println("\nYour fortune is: $fortune")
        if (fortune.contains("Take it easy")) return
    }

    while (!fortune.contains("Take it easy")){
        fortune = getFortuneCookie(getBirthday())
        println("\nYour fortune is: $fortune")
    }
```

Alguns recursos para mapear e filtrar os itens da lista:

```kotlin
fun eagerExample() {
    val decorations = listOf("rock", "pagoda", "plastic plant", "alligator", "flowerpot")

    val eager = decorations.filter { it[0] == 'p' }
    println(eager)

    val filtered = decorations.asSequence().filter { it[0] == 'p' }
    println("As sequence: $filtered")
    println(filtered.toList())

    val lazyMap = decorations.asSequence().map {
        println("map: $it")
    }

    println(lazyMap)
    println("first: ${lazyMap.first()}")
    println("all: ${lazyMap.toList()}")
}

fun curryExercise() {
    val spices = listOf("curry", "pepper", "cayenne", "ginger", "red curry", "green curry", "red pepper")

    val onlyCurry = spices.filter { it.contains("curry") }.sortedBy { it.length }
    println(onlyCurry)

    val startCendE = spices.filter { it[0] == 'c' && it[it.length - 1] == 'e' }
    val startCandE2 = spices.filter { it.first() == 'c' && it.last() == 'e' }
    val startCandE3 = spices.filter { it.startsWith('c') && it.endsWith('e') }
    val startCandE4 = spices.filter { it.startsWith('c') }.filter { it.endsWith('e') }
    println(startCandE4)

    val first3WithC = spices.take(3).filter { it.startsWith('c') }
    println(first3WithC)
}
```

Exemplo de alguns tipos de listas:

```kotlin
val symptoms = mutableListOf("white spots", "red spots", "not eating", "bloated", "belly up")

    symptoms.add("white fungus")
    symptoms.remove("white fungus")

    symptoms.contains("red") //false
    symptoms.contains("red spots") //true

    println(symptoms.subList(4, symptoms.size)) //belly up

    listOf(1, 5, 3).sum() //9
    listOf("a", "b", "cc.").sumBy { it.length } //5

    val cures = mapOf("white spots" to "Ich", "red sores" to "hole disease")

    println(cures.get("white spots"))
    println(cures["white spots"])

    println(cures.getOrDefault("bloating", "sorry I don't know"))
    cures.getOrElse("bloating"){"No cure for this"}
    
    val inventory = mutableMapOf("fish net" to 1)
    inventory.put("tank scrubber", 3)
    inventory.remove("fish net")
```



## Lambdas

Lambda é uma função anônima. Em vez de declarar uma função nomeada, declaramos uma função sem nome. Lambdas podem ser atribuídos a variáveis e serem argumentos de outras funções (high-level functions)

> O lambda é envolto sempre por chaves {}

```kotlin
//Neste caso, waterFilter é uma variável que vai guardar uma função
val waterFilter = {dirty: Int -> dirty / 2 }
//o lambda é separado pela seta de função. À esquerda ficam os argumentos do lambda, e à direita é o corpo do lambda

val waterFilter: (Int) -> Int = {dirty -> dirty / 2}
//Aqui é atribuído o tipo da variável, que será qualquer função que pede um (Int) como argmento e retorna um Int
//Com isso, não é mais necessário dizer que dirty é um Int dentro do lambda (inferência de tipo)
```

Ele é comumente usado dentro de funções e, neste caso, é preferível que seja passado como último parâmetro.

```kotlin
//Função de ordem superior: função que possui outra função como argumento/ parâmetro
fun updateDirty(dirty: Int, operation: (Int) -> Int): Int {
    return operation(dirty)
}

//há várias maneiras de usar o lambda na hora de chamar
fun dirtyProcessor() {
    dirty = updateDirty(dirty, waterFilter) // usa a variável diretamente
    dirty = updateDirty(dirty, ::feedFish) //passa uma referência da função nomeada com o :: na frente
    dirty = updateDirty(dirty) { dirty -> dirty + 50 } //sintaxe do último parâmetro: passa o lambda como argumento
}
```

A notação de tipo de função é mais legível, o que reduz os erros e mostra claramente em qual tipo é transmitido e qual tipo é retornado

```
gamePlay(rollDice2(4))

fun gamePlay(diceRoll: Int){
    println(diceRoll)
}

val rollDice2: (Int) -> Int = { sides ->
    if (sides == 0) 0
    else Random.nextInt(sides) + 1
}
```



### Diferença entre uma chamada de função normal e o lambda

```kotlin
val random1 = random()
val random2 = {random()}

//random1 tem um valor atribuído em tempo de compilação, e o valor nunca muda quando a variável for acessada.

//random2 tem um lambda atribuído em tempo de compilação, e o lambda é executado toda vez que a variável for referenciada, retornando um valor diferente.
```

## Classes

### Inicialização de variáveis

#### Lateinit vs. Lazy

| lateinit                                                     | lazy (delegação de propriedade)                              |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| **Não permite tipos nullables (?)**                          | Permite tipos que aceitam nulos                              |
| Precisa sempre de inicialização, ou dá UninitializesPropertyAccessException - driblada com a checagem isInitialized | Se a variável lazy for chamada em locais indevidos, dá NullPointerException |
| Mantém uma propriedade mutável                               | **Obriga o uso de propriedades imutáveis**                   |
|                                                              | Inicializa a propriedade na primeira vez que for utilizada e, nas próximas vezes, o valor é atribuído imediatamente, como se fosse um cache, o que pode causar um memory leak, então não é indicada para Fragments |

```kotlin
// Inicializa com o lateinit
private lateinit var recyclerView: RecyclerView

// Inicializa com o lazy
private val recyclerView: RecyclerView by lazy {
  note_list_recyclerview //or findViewById<RecyclerView>(R.id.note_list_recyclerview)
}
```



### Getters e Setters

Getters e setters são implícitos no Kotlin mas, quando precisa de um getter ou setter especial, é possível sobrescrever o padrão. Exemplo:

```kotlin
var volume: Int
        get() = width * height * length / 1000
        set(value) {
            height = (value * 1000) / (width * length)
        }
```

Quando as propriedades são chamadas, por debaixo dos panos elas chamam os getters e setters, então é uma expressão que precisa ser chamada com as chaves `"Volume: ${myAquarium.volume} liters"`

```kotlin
class Book (val isbn: Long) {
    var title = "default value"
    set(value) {
        if (!value.isNotEmpty()) {
            throw IllegalArgumentException("Title must not be empty")
        }
        field = value
    }
}
```



### Visibilidade/ encapsulamento

#### Pacote

- **public** - Padrão, o acesso é de qualquer lugar
- **private** - Só dentro do arquivo
- **internal** - No mesmo módulo

#### Membros de Classe

- **public**
- **private** - Dentro da classe, subclasses não podem ver
- **protected** - Dentro da classe, subclasses podem ver
- **internal** - Módulo pode ver

### Construtores

A maioria das classes só especifica um construtor (primário). Apesar disso, é possível sobrescrever e ter mais de um construtor com o `constructor()`. Quando se cria um construtor secundário (constructor), ele precisa chamar o construtor primário com o `this`.

É muito utilizado o bloco `init`, que funciona como um construtor, onde podemos adicionar lógica para inicializar as propriedades declaradas no construtor primário. Você pode ter mais de 1 bloco `init`, que sempre irá rodar antes do construtor secundário. A ordem dos inits é de cima para baixo, e se vai ser utilizada alguma propriedade dentro do init, ela precisa ser declarada antes do bloco init.

É uma boa prática no Kotlin só definir um construtor com parâmetros padrões para valores opcionais. Mas se for fazer um construtor secundário, considere fazer um método helper no lugar.

```kotlin
class Fish(val friendly: Boolean = true, volumeNeeded: Int) {
    val size: Int

    init {
        println("first init block")
    }
    
    //Não é uma boa prática usar construtores secundários, mas este é só um exemplo
    constructor() : this(true, 9) {
        println("running secondary constructor")
    }

    init {
        if (friendly) {
            size = volumeNeeded
        } else {
            size = volumeNeeded * 2
        }
    }

    init {
        println("constructed fish of size $volumeNeeded")
    }
}

//helper method to replace constructor
fun makeDefaultFish() = Fish(true, 50)

fun fishExample() {
    val fish = Fish(true, 20)
    println("Is the fish friendly? ${fish.size}. It needs volume ${fish.size}")
}
```

### Herança

Por padrão, as classes e variáveis do Kotlin não são abertas para serem herdadas, é preciso autorizar explicitamente isso com a palavra `open`.



### Object / Delegação de interface (Subclasse Singleton)

**Objeto em Kotlin não é uma instância de uma classe específica**. O Kotlin permite declarar uma classe na qual há apenas uma instância usando a palavra-chave `object`ao invés de `class`. Isso cria uma classe e somente uma instância dela, que será criada na primeira compilação. Isso é como se implementa **singleton** no Kotlin.

- Eles podem ter propriedades, métodos e um bloco `init`
- Essas propriedades e métodos podem ter modificadores de visibilidade
- Eles não podem ter construtores (primários ou secundários)
- Eles podem estender outras classes ou implementar uma interface

```kotlin

class Plecostomus(fishcolor: FishColor = GoldColor) : 
    FishAction by PrintingFishAction("a lot of algae"), 
    FishColor by GoldColor

interface FishColor {
    val color: String
}

object GoldColor : FishColor {
    override val color: String = "gold"
}

interface FishAction {
    fun eat()
}

class PrintingFishAction(val food: String): FishAction{
    override fun eat() {
        println(food)
    }
}
```

```kotlin
//pode ser utilizado para criar constantes, como no exemplo abaixo
object APIConstants {
    val baseUrl: String = "http://www.myapi.com/"
}
```

#### Interoperabilidade Java

No fim das contas, Kotlin converte um objeto em uma classe Java nos bastidores. Essa classe tem um campo privado `INSTANCE` que contém uma única instância (singleton) da classe.

```java
//Uma classe Java chamada Singleton foi gerada com um membro estático público final INSTANCE, incluindo a função pública myFunc()
Singleton.INSTANCE.myFunc()
```

Para fazer a função do objeto ou propriedade em Kotlin ser um membro estático da classe Java gerada, usamos a anotacão `@JvmStatic`.

```kotlin
object Singleton {
     
    @JvmStatic fun myFunc(): Unit {
        // do something
    }
}
```

Aplicando a notação `@JvmStatic`, o compilador transformou a função em estática. Agora, os callers Java podem chamá-la como uma chamada a um membro estático normal. Observe que usar o campo estático `INSTANCE`ainda funcionará.

```java
Singleton.myFunc()
```

### Equals

O equals do Kotlin não é como o do Java. Ele não referencia o endereço do objeto na memória, mas sim os valores.

```kotlin
val d1 = Decorations("granite")
val d2 = Decorations("slate")
val d3 = Decorations("slate")

println(d1.equals(d2)) //false
println(d3.equals(d2)) //true
```

### Anônimas

Classes anônimas no Kotlin são criadas usando `object: NomeDaClasse`, criando um novo objeto que implementará a classe.

```kotlin
fun refreshTitle() {
   _spinner.value = true
   repository.refreshTitleWithCallbacks(object: TitleRefreshCallback {
       override fun onCompleted() {
           _spinner.postValue(false)
       }

       override fun onError(cause: Throwable) {
           _snackBar.postValue(cause.message)
           _spinner.postValue(false)
       }
   })
}
```

### Sealed classes

Sealed classes são utilizadas quando queremos oferecer opções limitadas. Geralmente consideramos usar `enum` , mas pode ser que algumas restrições sejam demais para seu caso de uso, já que enums só podem ter uma única instância para cada valor e não podem ter mais informações sobre cada valor. Sealed classes combinam vantagens do enum com as classes abstratas: a liberdade de representação das classes abstratas com os tipos restritos dos enums. Um exemplo é para uma classe Result usada em um request, que deve passar dados em caso de sucesso e uma exceção em caso de erro:

```kotlin
sealed class Result<out T: Any> {
}
data class Success<out T: Any>(val data: T): Result<T>()
sealed class Error(val exception: Exception): Result<Nothing>() {
  class RecoverableError(exception: Exception): Error(exception)
  class NonRecoverableError(exception: Exception): Error(exception)
}
object InProgress: Result<Nothing>

fun handleResult(result: Result<Int>) {
  
   when(result){
    is Result.Success -> {...}
    is Result.Error.RecoverableError -> {...}
    is Result.Error.NonRecoverableError -> {...}   
  }
  
  // o when e o if, quando usados como expressão, são "monitorados" pedindo que a gente use todas as opções e dão erro de IDE indicando o que falta (usando o val action =)
  val action = when(result){
    is Result.Success -> {...}
    is Result.Error -> {...}
    Result.InProgress -> {...}
  }
  
  //or 
  
  when(result){
    is Result.Success -> {...}
    is Result.Error.RecoverableError -> {...}
    is Result.Error.NonRecoverableError -> {...}   
    Result.InProgress -> {...}
  }.exhaustive
}

//uma maneira de evitar colocar a expressão dentro de uma variável é criar uma extension function que vai garantir que a IDE gere branches para cada filho dentro da sealed class.

val <T> T.exhaustive: T
	get() = this

```

Assim como classes abstratas, sealed classes permitem que você represente hierarquias. As classes filhas podem ser de qualquer tipo, data class, object, uma classe normal ou mesmo outra sealed class. mas diferentemente de classes abstratas, as hierarquias de sealed classes devem ser feitas no mesmo arquivo, ou aninhada à classe pai. 

Alguns casos de uso para sealed classes são para mostrar estados de sucesso, erro e loading em uma ViewModel e também na camada de repositório/ remoto, para monitorar um request da API. 

Um outro caso menos usual é a construção de estado diferente de um mesmo Fragment/ Activity, quando há muitos parâmetros em um Bundle/ Intent, em que alguns são adicionados em alguns casos e outros não. Passar sealed classes devidamente nomeadas melhora bastante na legibilidade e é legal quando você está trabalhando em projetos herdados. Este [medium](https://medium.com/halcyon-mobile/simplify-your-android-code-by-delegating-to-sealed-classes-99304c509321) mostra como simplificar o código delegando para sealed classes e tem alguns exemplos parecidos com a situação apresentada acima.

#### Exhaustive

Uma outra vantagem das sealed classes é que tem como permitir que a IDE rastreie os filhos de uma sealed class, uma prática conhecida como exhaustive.

O when e o if, quando usados como expressão, monitoram a sealed class e quando há uma diferença entre as classes filhas e as opções do when/ if, a IDE apresenta um erro em compilação e dá a possibilidade do autocomplete para resolver as diferenças. É o caso do exemplo abaixo:

```kotlin
fun handleResult(result: Result<Int>) {
  val action = when(result){
    is Result.Success -> {...}
    is Result.Error -> {...}
    Result.InProgress -> {...}
  }
```

No Kotlin, podemos evitar colocar dentro de uma variável ao criar uma extension function conhecida como **exhaustive**, que mantém o monitoramento dos branches pela IDE:

```kotlin
fun handleResult(result: Result<Int>) { 
  when(result){
    is Result.Success -> {...}
    is Result.Error.RecoverableError -> {...}
    is Result.Error.NonRecoverableError -> {...}   
    Result.InProgress -> {...}
  }.exhaustive
}

val <T> T.exhaustive: T
	get() = this
```

Essa versão acima tem algumas desvantagens: o autocomplete vai gerar uma opção **exhaustive** para qualquer coisa, já que extende de um tipo muito amplo e a performance da extension poderia ser melhorada com o uso de inline. Uma abordagem legal é essa abaixo:

```kotlin
Do exhaustive when (sealedClass) {
  is SealedClass.First -> doSomething()
  is SealedClass.Second -> doSomethingElse()
}

object Do {
  //infix permite que a função seja chamada sem ponto ou brackets
    inline infix fun<reified T> exhaustive(any: T?) = any
}
```



## Constantes

`const val`é a maneira de criar constantes no Kotlin. A diferença entre `const val` e `val` é que o primeiro é sempre determinado no momento da compilação, enquanto com `val` , o valor pode ser determinado durante a execução do programa.

- Para `val`, podemos atribuir um valor return de uma função, porque o valor será atribuído durante a execução
- Para `const val` , como seu valor é setado no momento da compilação, não pode receber o valor de retorno de uma função. Só funciona em classes de alto nível e em classes declaradas com `object`, não com classes declaradas com `class`. Um exemplo é quando se precisa criar um arquivo que contenha somente constantes.

O Kotlin não tem um conceito de constante para classes regulares. Para ter constantes dentro de uma classe, é preciso envolvê-las em um `companion object`. Companion objects são inicializados do construtor estático na classe que os contém, ou seja, eles são criados quando o objeto for criado. Ao invés de ter uma classe estática, o que evita a instanciação, se usa o companion object. **Companion object** permite acessar atributos e métodos sem precisar criar um objeto da classe.

```kotlin
class MyClass{
	companion object{
		const val CONSTANT3 = "test"
	}
}
```

## Classes genéricas

Ao invés de fazer várias classes iguais para cada tipo de dado, são usadas classes genéricas, que podem se adequar a vários tipos de dados. É comumente representada pelo `<T>`, mas pode ser qualquer nome ou letra dentro de <>.

```kotlin
//aqui está um pouco restrito o tipo, mas abrange todos os que herdam de WaterSupply
class Aquarium<T : WaterSupply>(val waterSupply: T){
  fun addWater(){
    check(!waterSupply.needsProcessed){"water supply needs processed"}
    println("adding water from $waterSupply")
  }
}
```

### In e out genéricas

Quando se usa um tipo aberto é bem possivel que encontre problemas de compatibilidade de dados que nem sempre são variantes. Por isso, para evitar esse erro de `ClassCastException` em métodos no corpo da função ou da classe, se usa **in** ou **out**

- `in` só pode ser passado como parâmetro/ argumento, é como se tivesse permissão para setar.
- `out` são parâmetros de tipo que só ocorrem em valores retornados de funções ou na propriedade val - não pode passar como tipo de parâmetro/ argumento. É como se só chamasse o get(). Uma exceção é que Construtores podem passar um tipo `out`como argumento, mas funções nunca podem.

```kotlin
class Aquarium<out T : WaterSupply>(val waterSupply: T){
  fun addWater(){
    check(!waterSupply.needsProcessed){"water supply needs processed"}
    println("adding water from $waterSupply")
  }
}

fun addItemTo(Aquarium<WaterSupply) = println("item added")

fun genericExample(){
  val aquarium = Aquarium(TapWater())
  addItemTo(aquarium)
}

//IN é usado como argumento da função
interface Cleaner<in T: WaterSupply>{
	fun clean(waterSupply: T)
}
```

## Funções genéricas

```kotlin
fun <T: WaterSupply> isWaterClean(aquarium: Aquarium<T>){
  println("teste")
}
fun isWaterClean(aquarium) //a ID infere o tipo

//retorna true se o WatterSupply for do tipo R
//Reified significa ser real (inserindo o reified e o inline)
//declara um parâmetro de tipo R, mas torne-o real
inline fun <reified R: WaterSupply> hasWaterSupplyOfType() = waterSupply is R

aquarium.hasWaterSupplyOfType<TapWater>() //true
```

https://kotlinlang.org/docs/reference/generics.html#generics

## SAM (Método abstrato único)

SAM é uma interface com um método. Dois exemplos são **Runnable** e **Callable**:

```kotlin
interface Runnable{
	fun run()
}

interface Callable<T>{
  fun call(): T
}
```

Para usar esses SAM no Kotlin, podemos passar um lambda no lugar do SAM. O Kotlin criará o tipo certo de objeto sozinho.
