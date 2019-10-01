# Labirintum
Permite ao usuário armazenar as informações enviadas pelo acelerômetro e giroscópio presentes no Android periodicamente.

## Tabela de conteúdos

|                      Título                     |                                     Descrição                                     |
|-------------------------------------------------|-----------------------------------------------------------------------------------|
|       [Funcionalidades](#funcionalidades)       | Apresenta resumidamente as funcionalidades disponibilizadas pelo aplicativo.      |
|    [Introdução técnica](#introdução-técnica)    | Descreve como os dados foram coletados pelo aplicativo em nível de desenvolvedor. |
|[Como usar o aplicativo](#como-usar-o-aplicativo)| Explica como utilizar o aplicativo e configurar o registro dos dados.             |

## Funcionalidades

- Permite o salvamento de dados em um período de tempo definido.
- São registrados o período de tempo decorrido desde o início da gravaçãoe os valores dos 3 eixos.
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

<img src="https://i.imgur.com/K8KQtgb.jpg?1" alt="menu-principal" width="250"/>

Na tela inicial do aplicativo, existem três opções de botões: **Iniciar**, **Opções** e **Sobre**.

Para iniciar a gravação dos dados, basta clicar no botão **Iniciar** na parte inferior da tela.

<img src="https://i.imgur.com/eAFTz4l.jpg?1" alt="menu-nome-arquivo" width="250"/>

Ao clicar no botão **Iniciar**, será mostrada uma caixa de diálogo que receberá o nome do arquivo a ser
criado. Não é necessário colocar o nome do arquivo como sufixo, como **.csv** ou **.txt**, visto que já
é colocado automaticamente. Quando o nome do arquivo for definido, basta clicar no botão **Iniciar** da
caixa de diálogo para iniciar o registro dos dados.

<img src="https://i.imgur.com/vZJ4v2l.jpg?1" alt="menu-gravacao" width="250"/>

Em seguida, aparecerá uma mensagem indicando a pasta em que o arquivo foi salvo (sempre será na memória
interna dentro da pasta **LabirintumDados**). À medida em que os dados vão sendo salvos, uma lista
com os valores que estão registrados em memória será atualizada na tela, não só com o tempo decorrido mas
também com os valores dos eixos.

O usuário poderá pausar ou parar o registro dos dados, clicando respectivamente
nos botões **Pausar** e **Parar** na parte inferior da tela. Os dados também podem ser salvos com o aplicativo
em segundo plano. Parar o registro dos dados voltará ao menu principal e o arquivo poderá ser aberto no
diretório informado anteriormente. Dependendo das configurações definidas no menu **Opções**, também é
possível ver a representação dos dados em três gráficos, um para cada eixo, na parte superior da tela.

<img src="https://i.imgur.com/5ZGvGr5.jpg?1" alt="menu-configuracoes" width="250"/>

Ao voltar para o menu principal e selecionar o botão **Opções**, aparecerá o menu de configurações do aplicativo.
Nela, o usuário pode definir de quanto em quanto tempo os dados serão salvos no dispositivo
em milissegundos, na seção **Intervalo de gravação**, o formato de arquivo a ser salvo, CSV ou TXT, na seção
**Formato de arquivo**, ativar e definir o número máximo de linhas que cada arquivo salvo pode ter, na seção
**Limitar número de linhas salvas**, e escolher quais gráficos devem ficar visíveis durante o registros dos
dados, na seção **Definir gráficos visíveis**.

Por fim, ao clicar nos três pontinhos no canto superior direito
e selecionar a opção **Mostrar sensores disponíveis**, irá aparecer uma lista com os sensores presentes no dispositivo.
Caso o dado de algum sensor esteja apenas registrando valores nulos, é uma boa prática verificar se o sensor
está presente nessa lista.

<img src="https://i.imgur.com/fpZW1KQ.jpg?1" alt="menu-registros-anteriores" width="250"/>

De volta ao menu principal, ao clicar no botão de **Registros anteriores**, aparecerá todos os registros
salvos no dispositivo, baseados na pasta padrão de salvamento **LabirintumDados**, tanto com extensão **.csv**
quanto com extensão **.txt**. No card do arquivo, aparecerá o nome, a última data em que ele foi modificado 
e um botão no canto superior direito do card com uma opção para exluir o arquivo. Além disso, é possível
pesquisar por arquivos utilizando o campo de pesquisa. Os arquivos poderão ser ordenados de ordem crescente
ou decrescente e por data ou por nome, selecionando os botões do canto superior direito da tela.

<img src="https://i.imgur.com/uuFetlI.jpg?1" alt="menu-registros-anteriores" width="250"/>
<img src="https://i.imgur.com/sViUCV5.jpg?1" alt="menu-registros-anteriores" width="250"/>

Ao selecionar um card, aparecerá duas abas com opções de visualização: uma para visualizar os gráficos dos
três eixos do acelerômetro e do giroscópio e outro para visualizar o conteúdo do arquivo em forma de uma
tabela. Também é possível compartilhar o arquivo para outros aplicativos, como por examplo WhatsApp.

### Modo remoto

O modo remoto é um modo de utilizar o aplicativo por meio do recurso de Bluetooth. Ao selecionar essa opção,
clicando nos três pontinhos no canto superior direito do menu inicial, caso o Bluetooth não esteja ligado,
o usuário terá que permitir o ativamento do Bluetooth por meio de uma notificação que será mostrada na tela.
Em seguida, aparecerá uma mensagem perguntando se o dispositivo atual é o dispositivo que irá controlar o
registro dos dados (ou seja, o dispositivo do terapeuta) ou o dispositivo que irá registrar os dados (isto é,
o dispositivo do paciente).

Ao ser selecionada a opção do terapeuta, o dispositivo irá aguardar por conexões. Mantenha o dispositivo nesta
tela. Ao ser selecionada a opção do paciente, irá aparecer uma lista com os dispositivos que estão pareados via
Bluetooth com o seu dispositivo. Caso não veja o dispositivo que se deseja conectar, verifique se ambos estão
pareados antes de começar a gravação. Caso contrário, ao clicar no nome do dispositivo, o aparelho irá aguardar
por uma conexão do terapeuta. Após tudo ser concluído, o registro dos dados será iniciado automaticamente.

Na opção do paciente, não é possível pausar ou parar o registro dos dados, apenas na opção do terapeuta. Quando
o terapeuta encerrar a gravação, ambos os aplicativos voltam para o seu menu inicial.

Created my free logo at [LogoMakr.com](logoMakr.com).
