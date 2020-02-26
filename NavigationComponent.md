# Navigation Component

O Navigation Component faz parte do Jetpack para ajudar a implementar a navegação entre tela, além de reduzir a possibilidade de erro ao realizar alguma transação com fragments e melhorar a capacidade de testar a UI de forma isolada.

O componente de navegação é composto por 3 partes:

- Navigation Graph: um XML que contém todas as informações relacionadas à navegação em um local centralizado. Todo o fluxo fica nessa tela.
- NavHost: É um container vazio que mostra o conteúdo dos fragmentos do navigation graph. 
- NavController: é um objeto que gerencia a navegação do aplicativo em um NavHost. É o NavController que organiza a troca do conteúdo de destino no NavHost conforme os usuários se movem pelo aplicativo.

## Implementando o Navigation Component

### Dependências

```
//2.2.0-rc04
implementation "androidx.navigation:navigation-fragment-ktx:$nav_version"
implementation "androidx.navigation:navigation-ui-ktx:$nav_version"
```

### NavHost

O primeiro passo é determinar o container que vai abrigar os conteúdos do Navigation Graph e indicar o caminho que irá receber ele. Pode já criar o Navigation Android Resource File (XML).

```xml
<fragment
 android:id="@+id/nav_host_fragment"
 android:name="androidx.navigation.fragment.NavHostFragment"
 android:layout_width="match_parent"
 android:layout_height="match_parent"
 app:defaultNavHost="true"
 app:navGraph="@navigation/navigation" />
```

### Navigation Graph

No navigation.xml, você vai indicar o fluxo do seu app. Você pode usar o editor visual para isso, ao invés do xml, mas o codigo é composto por uma tag navigation que abriga tudo e define a home, além de ter o fragment e as actions, que são as setas que indicam o fluxo.

```xml
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/navigation"
    app:startDestination="@id/titleFragment">

    <fragment
        android:id="@+id/titleFragment"
        android:name="com.example.android.navigation.TitleFragment"
        android:label="fragment_title"
        tools:layout="@layout/fragment_title" >
        <action
            android:id="@+id/action_titleFragment_to_gameFragment"
            app:destination="@id/gameFragment" />
    </fragment>
    <fragment
        android:id="@+id/gameFragment"
        android:name="com.example.android.navigation.GameFragment"
        android:label="fragment_game"
        tools:layout="@layout/fragment_game" >
        <action
            android:id="@+id/action_gameFragment_to_gameWonFragment"
            app:destination="@id/gameWonFragment" />
        <action
            android:id="@+id/action_gameFragment_to_gameOverFragment2"
            app:destination="@id/gameOverFragment2" />
    </fragment>
</navigation>
```

### Nav Controller

Para de fato indicar que um botão ou uma ação leva à tela A ou B, você precisa referenciar na lógica usando o NavController.

```kotlin
//Essa é a ação direta de um botão
binding.playButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_titleFragment_to_gameFragment)
        )

//Essa está dentro de uma lógica condicional no botão
view.findNavController().navigate(R.id.action_gameFragment_to_gameWonFragment)
```

## Ajustes de navegação

Para configurar o botão de voltar, você pode configurar manualmente o **Pop Behavior**. Por exemplo, depois que você ganha ou perde o jogo, ao voltar você precisa ser capaz de ir à Tela Principal ao invés da última pergunta respondida.

Para isso, configurar o campo **Pop To** para o GameFragment e marcar o checkbox Inclusive, o que significa que ele removerá do backstack todos os outros destinos até que o indicado seja encontrado. O **Inclusive** marcado indica que também remove do backstack o destino indicado em Pop To. Ou seja, existem duas maneiras de resolver o exemplo: ou indica o GameFragment como Pop To e marca o Inclusive para que ele o ignore e vá para a tela anterior, ou indica diretamente o TitleFragment, sem marcar o Inclusive, assim ele ignorará o GameFragment da mesma maneira.

## Arguments substituem Intents

Além de levar para diferentes telas, o NavigationComponent permite que as actions transmitam valores com os arguments. Para que funcionem em um ambiente de LiveData e Recycler Adapters, precisam de muitas alterações.

```kotlin
//criar um atributo que vai monitorar a navegação no ViewModel da ORIGEM
private val _navigateToSelectedProperty = MutableLiveData<MarsProperty>()
val navigateToSelectedProperty: LiveData<MarsProperty>
    get() = _navigateToSelectedProperty

//A navegação é controlada por dois métodos, um que passa o bundle para mostrar os detalhes e outro que anula o bundle quando a navegação foi completada - ViewModel da ORIGEM
fun displayPropertyDetails(marsProperty: MarsProperty){
  _navigateToSelectedProperty.value = marsProperty
}

fun displayPropertyDetailsComplete(){
  _navigateToSelectedProperty.value = null
}
```

Depois, como neste exemplo estou usando um RecyclerView, preciso de um clickListener nele. Implemento ele primeiro, criando uma classe que terá o método `onClick()` , parecido com o que era feito com a interface no Java. Segundo, é importante que quando o adapter seja implementado no Fragment, ele passe um clickListener como parâmetro. Por isso, é adicionado ao construtor, da mesma maneira que no Java. Terceiro, implementa o click de fato no onBindViewHolder.

```kotlin
//2. Vai precisar passar o onClickListener no construtor no Fragment
class PhotoGridAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<MarsProperty, PhotoGridAdapter.MarsPropertyViewHolder>(DiffCallback) {

    class MarsPropertyViewHolder(private var binding: GridViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(marsProperty: MarsProperty) {
            binding.property = marsProperty
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MarsProperty>() {
        override fun areItemsTheSame(oldItem: MarsProperty, newItem: MarsProperty): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: MarsProperty, newItem: MarsProperty): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhotoGridAdapter.MarsPropertyViewHolder {
        return MarsPropertyViewHolder(GridViewItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: PhotoGridAdapter.MarsPropertyViewHolder, position: Int) {
        val marsProperty = getItem(position)
        //3. Chama o método ao click
        holder.itemView.setOnClickListener {
            onClickListener.onClick(marsProperty)
        }
        holder.bind(marsProperty)
    }
    
    //1. classe criada para OnClickListener
    class OnClickListener(val clickListener: (marsProperty: MarsProperty) -> Unit) {
        fun onClick(marsProperty: MarsProperty) = clickListener(marsProperty)
    }
}
```

Como a navegação é feita pelo Graph, precisa adicionar a tag `<argument>` no Fragment de destino, indicando o nome e o tipo.

```xml
	<!-- Esses dois fragments são os de destino, mas onde as ações ficam -->
	<fragment
        android:id="@+id/sleep_quality_fragment"
        android:name="com.example.android.trackmysleepquality.sleepquality.SleepQualityFragment"
        android:label="@string/sleep_quality_fragment"
        tools:layout="@layout/fragment_sleep_quality">
        <argument
            android:name="sleepNightKey"
            app:argType="long" />
        <action
            android:id="@+id/action_sleepQualityFragment_to_sleepTrackerFragment"
            app:destination="@id/sleep_tracker_fragment"
            app:launchSingleTop="false"
            app:popUpTo="@+id/sleep_tracker_fragment"
            app:popUpToInclusive="true" />
    </fragment>

    <fragment
        android:id="@+id/sleep_detail_fragment"
        android:name="com.example.android.trackmysleepquality.sleepdetail.SleepDetailFragment"
        android:label="fragment_sleep_detail"
        tools:layout="@layout/fragment_sleep_detail" >
        <argument
            android:name="sleepNightKey"
            app:argType="long" />
        <action
            android:id="@+id/action_sleepDetailFragment_to_sleep_tracker_fragment"
            app:destination="@id/sleep_tracker_fragment"
            app:launchSingleTop="false"
            app:popUpTo="@+id/sleep_tracker_fragment"
            app:popUpToInclusive="true" />
    </fragment>
```

No Fragment da origem, é preciso adicionar o OnClickListener na hora de instanciar o adapter. Isso é um pouco diferente do que fazia no Java, porque preciso vincular o objeto com o binding. Depois, preciso observar o status do navigateToSelectedProperty.

```kotlin
binding.photosGrid.adapter = PhotoGridAdapter(PhotoGridAdapter.OnClickListener {
            viewModel.displayPropertyDetails(it)
        })

viewModel.navigateToSelectedProperty.observe(this, Observer {
  if (null != it) {
    this.findNavController().navigate(OverviewFragmentDirections.actionShowDetail(it))
    viewModel.displayPropertyDetailsComplete()
  }
})
```

No fragment de destino, você poderá usar o `SleepDetailFragmentArgs` para pegar o argument e passar para o viewModelFactory, assim quando o ViewModel já inicializará com o atributo lá.

```kotlin
val application = requireNotNull(this.activity).application
val arguments = SleepDetailFragmentArgs.fromBundle(arguments!!)

// Create an instance of the ViewModel Factory.
val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao
val viewModelFactory = SleepDetailViewModelFactory(arguments.sleepNightKey, dataSource)
```


