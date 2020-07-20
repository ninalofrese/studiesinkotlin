# Push notification



## Broadcast receiver para tocar alarme



> **Broadcast receiver** serve para enviar e receber informações de outros aplicativos ou do sistema Android. Possui uma limitação de 10 segundos de processamento quando a API é chamada e o método onReceiver() é invocado. Por isso, é utilizado quando há a certeza de que o algoritmo que será executado a partir do onReceiver não chegará, em hipótese alguma, aos 10 segundos de execução.

## Canais de notificação

Notification channels são uma forma de agrupar as notificações. Ao ajuntar tipos similares de notificação, desenvolvedores e usuários podem controlar todas as notificações de um canal. Uma vez criado, pode ser utilizado para enviar qualquer número de notificações.

Canais precisam atender a uma hierarquia de importância, o que determina ativamente em como a notificação se portará no celular do usuário.

### **Channel Importance Levels**

| **User-visible importance level**                            | **Importance (Android 8.0 and higher)**                      | **Priority (Android 7.1 and lower)**                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| Makes a sound and appears as a heads-up notification (pops up at the top of the screen) | [IMPORTANCE_HIGH](https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_HIGH) | [PRIORITY_HIGH](https://developer.android.com/reference/androidx/core/app/NotificationCompat.html#PRIORITY_HIGH) / [PRIORITY_MAX](https://developer.android.com/reference/androidx/core/app/NotificationCompat.html#PRIORITY_MAX) |
| Makes a sound                                                | [IMPORTANCE_DEFAULT](https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_DEFAULT) | [PRIORITY_DEFAULT](https://developer.android.com/reference/androidx/core/app/NotificationCompat.html#PRIORITY_DEFAULT) |
| No sound                                                     | [IMPORTANCE_LOW](https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_LOW) | [PRIORITY_LOW](https://developer.android.com/reference/androidx/core/app/NotificationCompat.html#PRIORITY_LOW) |
| No sound and does not appear in the status bar               | [IMPORTANCE_MIN](https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_MIN) | [PRIORITY_MIN](https://developer.android.com/reference/androidx/core/app/NotificationCompat.html#PRIORITY_MIN) |

> **Note:** To support devices running Android 7.1 (API level 25) or lower, you must also call `setPriority()` for each notification, using a priority constant from the `NotificationCompat` class.

## Pending Intents

Pending Intent cede direitos a outra aplicação ou ao sistema para fazer uma operação em nome da sua aplicação. Uma pending intent por si só é uma referência a um token mantido pelo sistema que descreve os dados originais usados para recuperá-lo. Isso significa que, mesmo que o processo do seu aplicativo seja interrompido, o próprio PendingIntent permanecerá utilizável de outros processos aos quais foi entregue. Neste caso, o sistema usará a intenção pendente para abrir o aplicativo em seu nome, independente de o app estar ou não em execução.

```kotlin
// Uma intent normal é necessária
val contentIntent = Intent(applicationContext, MainActivity::class.java)

// A PendingIntent precisa do contexto da aplicação, o ID da notificação, o Intent criado e uma flag que especifique se quer criar um novo PendingIntent ou usar algum existente.
val contentPendingIntent = PendingIntent.getActivity(
  applicationContext,
  NOTIFICATION_ID,
  contentIntent,
  PendingIntent.FLAG_UPDATE_CURRENT //Modifica a notificação atual ao invés de criar uma nova
)
```



## Estilizando a notification

- BigTextStyle - mostra um bloco longo de texto, como o conteúdo de um e-mail quando expandido
- BigPictureStyle - mostra uma notificação maior que inclui anexada uma imagem grande
- InboxStyle - mostra um texto no estilo de chat
- MediaStyle - mostra controles para tocar alguma mídia
- MessagingStyle - mostra uma notificação maior que inclui múltiplas mensagens entre qualquer número de pessoas

## Ações nas notificações

É possível adicionar botões que completem alguma ação direto nas notificações. Uma notificação pode ter até 3 ações, que não podem duplicar a ação que acontece quando a notificação é clicada.

Para adicionar um botão de ação, passe uma PendingIntent para o método `addAction()` no builder. É similar a configuração do click normal da notificação, exceto que, ao invés de abrir uma Activity, pode ser feita uma variedade de outras coisas, por exemplo, começar um BroadcastReceiver que fará um trabalho no background para que a ação não interrompa o app que já está aberto.

## Notification badges

As notification badges são aqueles pontos que aparecem junto ao ícone do app para mostrar que há notificações ativas. Os usuários podem clicar e segurar para ver as notificações. Elas aparecem por padrão mas, caso essas badges não façam sentido para suas notificações, pode desativar para um canal.

# Firebase Cloud Messaging

Primeiro, é preciso criar um novo projeto no Firebase. É importante que o package do projeto seja único, então a esta altura do projeto, o pacote dele deve estar bem definido.

1. Vá ao [Firebase Console](https://console.firebase.google.com/) 
2. Clique em **Add project** e adicione um nome de projeto
3. Clique em **Continue**
4. Pule o setup do Google Analytics, desmarcando o **Enable Google Analytics for this project**
5. Clique em **Create Project** para finalizar a criação do projeto

Depois, precisa registrar o projeto no Firebase. 

1. Na tela do projeto, clique no **ícone do Android** para começar o setup.
2. Coloque o package do projeto (confira o ID disso em AndroidManifest > manifest tag > package tag). Ele não poderá ser alterado
3. Clique em **Register app**

Depois disso, é preciso adicionar a configuração do Firebase para o projeto.

1. Faça o **download do google-services.json** para o app. A indicação padrão é que ele fique na raiz do app, mas  pode colocar ele em algum lugar diferente e depois configurar. Esse arquivo é muito importante, então cuidado ao dar push nele junto com o projeto (coloque em um local que não vai subir, ou dê gitignore nele)
2. No `build.gradle (projeto)` adicione `classpath 'com.google.gms:google-services:4.3.2'` no bloco de dependencies (buildscript)
3. No `build.gradle (app)` adicione `apply plugin: 'com.google.gms.google-services' ` no fim do arquivo

## Service

O service é necessário para conectar o app com o Notifications composer do Firebase. Ela estende do `FirebaseMessagingService() ` e tem dois métodos nesse arquivo que são importantes

- `onNewToken()` - Chamado automaticamente se o serviço está registrado no AndroidManifest. Esse método é chamado quando você roda o app pela primeira vez e toda vez que o Firebase envia um novo token pro seu app. Um token é uma chave de acesso para o backend do projeto do Firebase. É gerada especificamente para o aparelho. Com esse token, o Firebase sabe para qual cliente deve enviar mensagens. É desta forma que o Firebase também sabe se o cliente é válido e se tem acesso a este projeto.
- `onMessageReceived` - Chamado quando o app está rodando e o Firebase envia uma mensagem para o app. Esse método recebe um objeto `RemoteMessage` que carrega uma notificação ou data message payloads.

Mais sobre `onNewToken()`: quando o backend do Firebase gera uma chave nova ou renovada, o método `onNewToken()` é chamado, com o novo token como parâmetro. Se você quer enviar mensagens específicas para um aparelho ou um grupo de aparelhos, você precisa acessar esse token estendendo `FirebaseMessagingService` e sobrescrevendo o `onNewToken()`.

O Service precisa ser registrado no AndroidManifest e adicionado como um intent filter para que esse serviço receba mensagens enviadas pelo Firebase Cloud Messaging (FCM). A última parte do metadata declara o canal correto como padrão para o Firebase. É interessante que seja criado um canal à parte das notificações internas para o Firebase, assim se o usuário quiser desativar um deles, não desativa o outro.

```xml
        <service
            android:name=".MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        <!-- [START fcm_default_icon] -->
        <!--
        Set custom default icon. This is used when no icon is set for incoming notification messages.
        See README(https://goo.gl/l4GJaQ) for more.
        -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@drawable/common_google_signin_btn_icon_dark" />
        <!--
        Set color used with incoming notification messages. This is used when no color is set for the incoming
        notification message. See README(https://goo.gl/6BKBk7) for more.
        -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_color"
            android:resource="@color/colorAccent" /> <!-- [END fcm_default_icon] -->
        <!-- [START fcm_default_channel] -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="@string/breakfast_notification_channel_id" />
        <!-- [END fcm_default_channel] -->
```

## Publish/ subscribe model

As mensagens por topic do FCM é baseada no modelo publish/ subscribe. Um bom exemplo é um aplicativo de mensagens, imagina se o app checar por novas mensagens a cada 10 segundos. Não só consumirá muita bateria como também drenará recursos de rede e gerará uma carga desnecessária no servidor do app. Ao invés disso, um client device pode se inscrever e ser notificado quando tem novas mensagens para serem entregues ao app.

**Topics** permitem o envio de mensagens para vários dispositivos que se inscreverem naquele tópico em particular. Para clients, tópicos são data sources específicos em que o cliente está interessado. Para o server, tópicos são grupos de dispositivos que optaram por receber updates de um data source específico. Tópicos podem ser usados para apresentar categorias de notificações, como notícias, previsão do tempo e resultados de esportes. 

Para dar **subscribe** em um tópico, o client app chama o método `subscribeToTopic()` do FCM com o nome do tópico. Essa chamada pode resultar em duas coisas. Se for sucesso, o callback `onCompleteListener` vai ser chamado com a mensagem de inscrição. Se falhar, o callback receberá uma mensagem de erro no lugar.

O app pode inscrever automaticamente no tópico, mas também pode ter um lugar onde o usuário tem esse controle.

## Data Messages

As mensagens do FCM também podem conter um data payload que processa a mensagem no client app, e para isso use data messages ao invés de mensagens de notificação.

Para lidar com data messages, é preciso lidar com data payload no método `onMessageReceived()` no `MyFirebaseMessageService`. O payload fica guardado na propriedade `data` do objeto `remoteMessaging`. Tanto o objeto `remoteMessage` e a propriedade `data` podem ser nulas.

> O máximo de tamanho do payload é 4KB (exceto quando envia mensagens do Firebase Console, o que força um limite de 1024 caracteres)

No FCM, por exemplo, no Notification composer, você envia dados personalizados na etapa 4 (outras opções (opcional)), 

## Mensagens em foreground e background

Quando um dispositivo client recebe uma mensagem que inclui tanto notificação quanto data payloads, o comportamento do app depende se o app está no background ou foreground no dispositivo:

| Estado do app | Notificação         | Dados               | Ambos                                                        |
| :------------ | :------------------ | :------------------ | :----------------------------------------------------------- |
| Foreground    | `onMessageReceived()` | `onMessageReceived()` | `onMessageReceived()`                                          |
| Background    | Bandeja do sistema  | `onMessageReceived()` | Notificação: bandeja do sistema <br />Dados: nos extras do intent. |

- Se estiver no background, se a mensagem tiver notificação, ela vai mostrar na área de notificações. Se a mensagem também tiver um data payload, o data payload vai ser lidado pelo bundle da activity quando o usuário tocar na notificação. É possível adicionar o destino com a tag `<action android:name="abc" />` dentro do `<intent-filter>` do AndroidManifest, mas talvez isso possa ser tratado como um argument no Navigation Component também. 
- Se o app estiver em foreground, tudo que vier deve ser tratado dentro do método `onMessageReceived()`, a notificação não aparece automaticamente. O app precisa decidir como lidar tanto com a notificação quanto o data payload no método `onMessageReceived()`.
- Se o app está em background e a notificação precisa ser lidada pela bandeja do sistema, ele vai mostrar a notificação com as configurações padrões do Android junto com as personalizações feitas no manifest. Essa mesma notificação, em foreground, só vai aparecer se o app decidir mostrar no método `onMessageReceived()`. Essa funcionalidade faz com que o app possa lidar silenciosamente com a notificação/ data ou disparar uma notificação.ação.

***

https://firebase.google.com/docs/cloud-messaging/android/client

[Comportamento do app quando recebe uma notification ou um data payload](https://firebase.google.com/docs/cloud-messaging/android/receive#sample-receive)
