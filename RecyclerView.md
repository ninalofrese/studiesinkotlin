## Uso do DiffUtil ao invés do notifyDataSetChanged()

É muito comum, ao atualizar os itens passados ao adapter, usar o método `notifyDataSetChanged()`para atualizar a lista. Este método irá recriar todos os itens do RecyclerView, o que pode causar alguns glitches (piscadas) e é na maioria das vezes desnecessário, porque recarrega TODOS os itens da lista, ao invés de um ou outro. Imagina o efeito disso em grandes listas?

É por isso que existe o DiffUtil, que tem um ItemCallback, o que permite recarregar valores de somente um item. É preciso criar uma classe que herde de DiffUtil para sobrescrever o padrão.

```kotlin
class SleepNightDiffCallback : DiffUtil.ItemCallback<SleepNight>() {
    override fun areItemsTheSame(oldItem: SleepNight, newItem: SleepNight): Boolean {
        return oldItem.nightId == newItem.nightId
    }

    override fun areContentsTheSame(oldItem: SleepNight, newItem: SleepNight): Boolean {
        return oldItem == newItem
    }
}
```

Depois, basta adicionar no ViewHolder.

```kotlin
class SleepNightAdapter(val clickListener: SleepNightListener) :
    ListAdapter<SleepNight, SleepNightAdapter.ViewHolder>(SleepNightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!, clickListener)
    }

    class ViewHolder(val binding: ListItemSleepNightBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: SleepNight,
            clickListener: SleepNightListener
        ) {
            binding.sleep = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val binding = ListItemSleepNightBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ViewHolder(binding)
            }
        }
    }

}
```



------

Existem muitas outras formas e situações de usar o DiffUtil:

https://antonioleiva.com/recyclerview-diffutil-kotlin/

https://proandroiddev.com/advanced-usage-of-diffutil-with-kotlin-and-rxjava2-2622e08b552b

