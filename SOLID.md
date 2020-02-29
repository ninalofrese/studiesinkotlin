# Conceito

- S - Single Responsability

- O - Open-Closed

- L - Liskov Substitution

- I - Interface Segregation

- D - Dependency Inversion

## Single Responsability Principle

As melhores classes são aquelas que possuem uma única responsabilidade bem feita. O número de métodos **não necessariamente** adiciona mais responsabilidades a uma classe. A responsabilidade de uma classe é relacionada aos clientes que usam ela, com os interessados na API pública da classe.

- Um ator servido por classe
- Uma razão para alterar por classe
- Visibilidade de efeitos colaterais

Tem um problema que pode acontecer quando uma classe tem mais de um propósito, é quando tem a cohabitação no mesmo conjunto de arquivos ou no mesmo arquivo não existe uma garantia de que alterar um não influencie no outro.

> Cada classe ou módulo deve ter uma, e apenas uma responsabilidade dentro do sistema. Se uma classe faz mais de uma coisa, deve ser dividida em outras classes.

Exemplos de violação:

- Uma activity falar diretamente com o banco de dados
- Fazer conta dentro da onde se lida com o banco de dados (storage procedure)
- Fragment que chama a REST API direto (Retrofit e model)
- Popula a visualização do JSON direto na tela
- Acessar a instância de um singleton dentro de um método local. Exemplo: Chamar o Firebase.getInstance() (Singleton) direto dentro de um método. **O correto é a instância (singleton ou não) ser chamada no construtor.** Ou seja, você pega a instância do Firebase.getInstance no construtor. Aí você consegue colocar abstração no meio, inversão de controle, etc.
- Acesso estático em geral denunciam uma violação deste princípio, mas porque você está agrupando um comportamento que não é delegado em uma relação de composição

Dicas para entrar em conformidade com o SRP:

- Identificar atores
- Classes menores e mais colaboradores por classe
- Pequenas funções/ métodos por classe/ módulo
- Evitar acesso estático
- Design patterns

## Dependency Inversion Principle

Acontece quando a gente faz o uso próprio de indireções, o que permite desacoplar a dependência do código-fonte e do runtime. Exemplo: Carro (turnOn()) usa Engine (start()) para iniciar o motor. É um exemplo de composição

- Dependência do código-fonte: o nome Engine aparece no arquivo Car.java.
- Dependência do runtime: a instância de Car precisará da instância de Engine. Se ele não conseguir criar ou acessar essas instâncias, o app irá crashar.

Quando se monta uma árvore de dependências, o problema é que é preciso recompilar em cascata. A linguagem GO foi criada pensando em resolver este problema. Para se resolver este problema, se usa interfaces, assim você tira a interface da equação do runtime e não instancia direto o método da interface, somente usa-a para tipar. Isso é chamado de **inversão de dependência**. Sendo assim, ela não precisa recompilar em cascata, somente no que for uma implementação direta da interface.

- É a arte das indireções
- Dependências de código-fonte são desacopladas das dependências de runtime, o que permite escalar a aplicação
- Possibilita API design plugin-like.
- Classes concretas dependem de classes abstratas, nunca o contrário
- Funcionalidades voláteis dependem de funcionalidades estáveis
- Funcionalidades específicas de framework dependem da lógica de negócio



## Interfaces Segregation Principle

É um conceito prático de single responsability aplicado a interfaces, em teoria. A interface não pode "vazar" abstrações, ou seja, os clients não precisam saber de coisas que eles não precisam. Ou seja, as interfaces não precisam centralizar métodos que serão utilizados em implementações diferentes. É melhor que a interface agrupe os elementos mais importantes. É melhor que várias interfaces tenham poucos métodos, e o cliente vai enxergar somente o que interessa para ele, conforme as interfaces que ele precise implementar.

- Use interfaces para informar funcionalidades
- Muitas interfaces específicas é melhor do que uma interface genérica
- Uma interface só expõe os métodos que a classe dependente precisa, não mais

## Lyskov Substitution Principle

Tem a ver com a formação de subtipos, é considerado na prática para a construção de subtipos efetivamente. Um bom app de orientação a objetos precisa garantir que todos os tipos abertos são perfeitamente substituíveis. Ex: você deve esperar o mesmo comportamento de um supertipo ou subtipo, o subtipo não pode sobrescrever um comportamento do supertipo que acabe atropelando quando for chamado.

- Classes low-level podem ser substituídas sem afetar as classes high-level
- Atingido usando classes abstratas e interfaces

Um exemplo é ter uma activity e sobrescrever o método onStart() sem referenciar o super. Isso viola o princípio.

Ex: Retangulo tem altura e largura e calcula uma area e Quadrado herda dele e sobrescreve o setWidth() e o setHeight(), ignorando o super em um deles. Se na hora de calcular você receba um parâmetro do tipo Retângulo, mas use uma instância de Quadrado para adicionar ao cálculo da área, você terá o resultado errado.

Você pode usar um _instanceof_, mas isso acaba gerando um efeito indesejado, porque agora o cliente vai ter que saber esse desdobramento. O jeito certo, na verdade, é que Quadrado e Retangulo devem ser criados sem nenhuma relação de tipo dentro do sistema.

> "Em um sistema de software, abstrações não podem preservar os mesmos relacionamentos que suas representações do mundo real não teriam."
>
> Regra da Representatividade

- Crie seus subtipos com sabedoria, principalmente os que você não controla
- Subtipos precisam ser perfeitamente substituíveis
- Lembre da regra da representatividade
- Evite o _instanceof_ a todo custo

## Open-Closed Principle

Tem a ver com a questão de propósito do sistema de software, o centro moral de um sistema. O bom sistema de software precisa ser aberto para extensão, fechado para modificação

- Aberto para extensões: significa que podemos adicionar novas features de uma forma mais fácil
- Fechado para modificações: significa que podemos alcançar isso sem mudanças de código-fonte (ou o mínimo de alterações em código-fonte necessárias)
- Se uma nova funcionalidade precisa ser adicionada, deve ser adicionada a uma extensão da classe.

A expectativa é que esse princípio seja um reflexo da visão real do usuário e/ou a empresa do projeto do usuário. É mais sobre a complexidade de estruturação do usuário em níveis mais simples de entender, com uma melhor saúde do produto, melhorando inclusive todos os stakeholders envolvidos.

**Como lidar com a entropia do software?**

> Entropia é quando algo começa ordenado e vai se tornando desordenado

| Big design up-front <br />(análise de sistemas, antigamente era mais comum) | Agile design                           |
| ------------------------------------------------------------ | -------------------------------------- |
| Efetivo para escopos pequenos                                | Sem escopo definido                    |
| Não é escalável                                              | Aprendizado retrospectivo              |
| Engenharia em excesso                                        | Exploração incremental sobre o domínio |
|                                                              | Adaptação e re-trabalho                |

As duas coisas são importantes e devem ser utilizadas juntas.

- O centro moral do software
- Reflete o controle da entropia
- As violações dos outros princípios + códigos ruins são fatores que comprometem a OCP
- Os stakeholders têm um pouco mais de previsibilidade
- Não pode ser 100% atingido na prática, mas deve ser buscado

***

Os princípios DIP e LSP são fundamentais para o design de um sistema de maneira que os elementos de alto nível (comportamentos) não dependam diretamente dos detalhes de baixo nível (implementações). Ou seja, separar comportamento de implementação.
