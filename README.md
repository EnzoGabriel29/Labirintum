# Labirintum App
Permite ao usuário armazenar as informações enviadas pelo sensor de aceleração presente no Android periodicamente.

## Tabela de conteúdos

|                      Título                     |                                     Descrição                                     |
|-------------------------------------------------|-----------------------------------------------------------------------------------|
|       [Funcionalidades](#funcionalidades)       | Apresenta resumidamente as funcionalidades disponibilizadas pelo aplicativo.      |
|    [Introdução técnica](#introdução-técnica)    | Descreve como os dados foram coletados pelo aplicativo em nível de desenvolvedor. |
|[Como usar o aplicativo](#como-usar-o-aplicativo)| Explica como utilizar o aplicativo e configurar o registro dos dados.             |

## Funcionalidades

- Permite o salvamento de dados em um período de tempo definido.
- São registrados o horário em que foi o dado foi salvo e os valores dos 3 eixos.
- Os dados são salvos em um arquivo no dispositivo, podendo ser CSV ou TXT.
- A gravação de dados pode ser iniciada em outro dispositivo que esteja pareado via Bluetooth.

## Introdução técnica

> Informações baseadas em https://developer.android.com/reference/android/hardware/SensorEvent.html

O sistema de coordenadas utilizado pelo sensor do Android é definido em relação à tela do telefone
em sua orientação padrão (modo retrato). Os eixos não são alterados quando a orientação da tela do
dispositivo muda. O eixo X é horizontal e aponta para a direita, o eixo Y é vertical e aponta para
cima e o eixo Z aponta para o lado de fora da face frontal da tela.

![](https://i.imgur.com/j042mjz.png)

O sensor retorna os valores registrados em uma *array*. O comprimento e o conteúdo da *array* de
retorno dependem de qual tipo de sensor está sendo monitorado. No caso do acelerômetro, o sensor mede
a aceleração aplicada ao dispositivo e retorna os valores dos três eixos em variáveis do tipo `Double`
e na unidade do Sistema Internacional, **m/s²**. Como a força da gravidade sempre está influenciando na
medição da aceleração do dispositivo, quando o dispositivo está em repouso em uma mesa, o acelerômetro
lê um valor de 9,81 m/s².

Como o acelerômetro do dispositivo retorna os últimos valores registrados de cada eixo e é preciso que
haja essa medição em tempo real, pode-se calcular a variação da aceleração de cada eixo por meio da
variação entre a última aceleração registrada e a aceleração atual. Portanto, se o dispositivo estiver
em repouso em uma mesa, ao invés de registrar 9,81 m/s², ele irá registrar 0,00 m/s², visto que não houve
variação de aceleração pelo fato do dispositivo estar em repouso.

O próximo passo é o registro dessas informações em memória. O usuário pode pausar ou parar o registro
desses dados. Os dados são registrados com data e hora de registro, no formato `DD/MM HH:MM:SS`, e com
os valores de aceleração de cada eixo do dispositivo, no formato `%.2f`.

## Como usar o aplicativo

![](https://i.imgur.com/SU6aniq.png?1)

Na tela inicial do aplicativo, os valores registrados pelo acelerômetro serão apresentados em tempo real.
Para iniciar a gravação dos dados, basta clicar no botão **Iniciar** na parte inferior da tela.

![](https://i.imgur.com/VnKFzKr.png)

Ao clicar no botão **Iniciar**, será mostrada uma caixa de diálogo que receberá o nome do arquivo a ser
criado. Não é necessário colocar o nome do arquivo como sufixo, como **.csv** ou **.txt**, visto que já
é colocado automaticamente. Quando o nome do arquivo for definido, basta clicar no botão **Iniciar** da
caixa de diálogo para iniciar o registro dos dados.

![](https://i.imgur.com/2EiVE2W.png)

Em seguida, aparecerá uma mensagem indicando a pasta em que o arquivo foi salvo (sempre será na memória
interna dentro da pasta **AccelerometerSaveData**). À medida em que os dados vão sendo salvos, uma lista
com os valores que estão registrados em memória será atualizada na tela, não só o horário mas também os
valores dos eixos. O usuário poderá pausar ou parar o registro dos dados, clicando respectivamente nos
botões **Pausar** e **Parar** na parte inferior da tela. Os dados também podem ser salvos com o aplicativo
em segundo plano. Parar o registro dos dados voltará ao menu principal e o arquivo poderá ser aberto no
diretório informado anteriormente.

Ao clicar nos três pontinhos no canto superior direito do menu principal, aparecerão três opções disponíveis.

### Configurações

Nas configurações, o usuário pode definir de quanto em quanto tempo os dados serão salvos no dispositivo
em milissegundos, na seção **Intervalo de gravação**, o formato de arquivo a ser salvo, CSV ou TXT, na seção
**Formato de arquivo** e ativar e definir o número máximo de linhas que cada arquivo salvo pode ter, na seção
**Limitar número de linhas salvas**.

### Ativar Modo Voz

TBA

### Ativar Modo Remoto

O modo remoto é um modo de utilizar o aplicativo por meio do recurso de Bluetooth. Ao selecionar essa opção,
caso o Bluetooth não esteja ligado, o usuário terá que permitir o ativimento do Bluetooth por meio de uma
notificação que será mostrada na tela. Em seguida, aparecerá uma mensagem perguntando se o dispositivo atual
é o dispositivo que irá controlar o registro dos dados (ou seja, o dispositivo do terapeuta) ou o dispositivo
que irá registrar os dados (isto é, o dispositivo do paciente).

Ao ser selecionada a opção do terapeuta, irá aparecer uma caixa de diálogo com os dispositivos que estão
pareados com o celular no momento. Caso não veja o dispositivo que se deseja conectar, verifique se ambos estão
pareados antes de começar a gravação. Caso veja o nome do dispositivo, antes de clicar em seu nome, tenha certeza
de que o outro dispositivo selecionou a opção do paciente. Quando o outro usuário selecionar essa opção no outro
dispositivo, irá aparecer uma notificação pedindo permissão para o dispositivo ficar visível durante 5 minutos.
Concedida a permissão, o dispositivo do terapeuta pode selecionar o nome do dispositivo do paciente e o registro
dos dados irá automaticamente ser iniciado.

Na opção do paciente, não é possível pausar ou parar o registro dos dados, apenas na opção do terapeuta. Quando
o terapeuta encerrar a gravação, ambos os aplicativos voltam para o seu menu inicial.

Created my free logo at [LogoMakr.com](logoMakr.com).