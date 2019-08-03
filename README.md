# Labirintum App

Permite ao usuário armazenar as informações enviadas pelo sensor de aceleração presente no Android periodicamente.

## Funcionalidades

- Permite ao usuário salvar os dados em um período de tempo de 0.5 segundos.
- São registrados o horário em que foi o dado foi salvo e os valores dos 3 eixos.
- Os dados são salvos em um arquivo CSV no dispositivo.
- O usuário agora pode iniciar a gravação de dados em outro dispositivo que esteja pareado via Bluetooth.

## Introdução

> Informações baseadas em https://developer.android.com/reference/android/hardware/SensorEvent.html

O sistema de coordenadas utilizado pelo sensor do Android é definido em relação à tela do telefone em sua orientação padrão (modo
retrato). Os eixos não são alterados quando a orientação da tela do dispositivo muda.

O eixo X é horizontal e aponta para a direita, o eixo Y é vertical e aponta para cima e o eixo Z aponta para o lado de fora da face
frontal da tela. Neste sistema, as coordenadas atrás da tela têm valores negativos de Z.

![](https://i.imgur.com/j042mjz.png)

O sensor retorna os valores registrados em um vetor. O comprimento e o conteúdo desse vetor dependem de qual tipo de sensor está sendo
monitorado. No caso do acelerômetro, o sensor mede a aceleração aplicada ao dispositivo e retorna os valores dos três eixos em variáveis
do tipo float e na unidade do Sistema Internacional, m/s².

Como a força da gravidade sempre está influenciando na medição da aceleração do dispositivo, quando o dispositivo está em repouso em uma
mesa, o acelerômetro lê um valor de 9,81 m/s² (aproximadamente o valor da força da gravidade).

## Implementação

Como o acelerômetro do dispositivo retorna sempre os últimos valores registrados de cada eixo e precisamos dessa medição em tempo real,
podemos calcular a variação da aceleração de cada eixo por meio da fórmula Δa = a₂ - a₁ onde a₁ é a última aceleração registrada e a₂ é
a aceleração atual. Portanto, se o dispositivo estiver em repouso em uma mesa, ao invés de registrar 9,81 m/s², ele irá registrar 0,00
m/s², visto que não houve variação de aceleração pelo fato do dispositivo estar em repouso.

O próximo passo é guardar essas informações em memória. O usuário pode pausar a gravação dos dados ou parar o registro desses dados. Os
dados são registrados com data e hora de registro (no formato DD/MM HH:MM:SS) e com os valores de aceleração de cada eixo do dispositivo.

## Configurações

Nas configurações, o usuário pode definir

- de quanto em quanto tempo os dados serão salvos no dispositivo, em milissegundos;
- o formato de arquivo a ser salvo (CSV ou TXT); e
- ativar e definir o número máximo que cada arquivo salvo pode ter.
