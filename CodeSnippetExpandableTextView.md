# Como fazer um Expandable TextView

Meu problema era o seguinte: eu tinha um texto muito grande para ser exibido integralmente logo de cara, mas não podia deixar ele "cortado" para sempre no ellipsize da TextView. Por isso, depois de muito pesquisar na internet e tentar várias Custom Views, fazer uma extension function foi o que mais funcionou para mim.

Minha situação inicial eram duas TextViews, uma que teria o label e outra que teria o texto dessa label. Era algo como "Label: Texto". Só que o texto poderia ser gigante, então estava usando o **ellipsize: end** com um número máximo de linhas. O resto do texto não fica disponível ao click e, para o meu usuário, não fica tão óbvio que apenas três pontinhos expandem o texto completo.

A primeira adaptação que vi que precisava fazer era juntar os dois TextViews em um. Se o texto seria expansível, a label não poderia ficar com um espaço branco embaixo dela enquanto o texto teria umas 20 linhas quando aberto. Por isso, eliminei um TextView e coloquei a label como um prefixo da TextView. Criei a extensão `getFullText()` que faz essa junção em uma `SpannableStringBuilder`. Ela usa a `styleSpan()` que criei para facilitar a formatação, adicionando texto e/ou bold de uma vez só.

Para criar o texto abreviado, uso o método `setExpandText()`, que mede o texto completo de retorno da `getFullText()` e cria um novo baseado no tamanho da tela e no número máximo de linhas, juntando com o texto de "ver mais". Nessa função muita coisa pode dar errado, porque medir telas no Android não é uma tarefa fácil quando você está criando a view. Por isso, implementei duas medidas de segurança. A primeira é considerar possíveis problemas que venham do XML, caso não tenha um `maxLines` definido, ou caso tenha um `ellipsize` definido. Isso deve ser o suficiente para evitar `IndexOutOfBoundsException`, mas caso algo extraordinário aconteça, usaremos o padrão de TextView com ellipsize.

A ação ao clicar na TextView é bem simples: o método `setExpandAction()` vai expandir para o texto completo (o número máximo de linhas pode ser ampliado). Resolvi não permitir abreviar esse texto depois de expandido justamente por questões de performance, já que para fazer isso tem que medir novamente a tela e pode causar alguns glitchs no comportamento. Para o meu caso de uso, apenas abrir é o ideal.

Esses dois métodos são chamados no método principal `setAsExpandable()`, que possui duas medidas para evitar perdas de performance. A primeira é o uso do `post()`, que vai rodar o código sem travar a UI Thread. A segunda é chamar os método apenas depois da tela ter sido medida, para não tentar trabalhar com valores nulos e dar alguma Exception.

Para usar essas extensions na TextView é só usar o `setAsExpandable()` passando o texto, a view do texto ou o grupo à qual ela pertence (que será escondido se o texto for vazio ou nulo) e o texto da label que vai preceder o texto.



<script src="https://gist.github.com/ninalofrese/885d45f942e1d696e02b2435c6a65e98.js"></script>

O meu uso nesse caso foi em um TextView na tela, por isso é necessário testar a performance em uma RecyclerView e fazer ajustes de acordo. 




# Links

- [Gist com todo o código deste artigo](https://gist.github.com/ninalofrese/885d45f942e1d696e02b2435c6a65e98)
- [Baseado no código deste projeto que replica a interface do Instagram Feed - RecyclerView](https://github.com/alishari/Instagram-Feed-UI)