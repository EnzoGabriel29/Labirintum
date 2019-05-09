# Acelerômetro visual para Android
Permite ao usuário armazenar as informações enviadas pelo sensor de aceleração presente no Android periodicamente.

## Features

- Possui dois modos de visualização de dados: texto e gráfico de barras.
- Permite ao usuário salvar os dados em um período de tempo definido.
- São registrados o horário em que foi o dado foi salvo e os valores dos 3 eixos.
- Os dados são salvos em memória, usando o SQLite.

## Introdução

> Informações baseadas em https://developer.android.com/reference/android/hardware/SensorEvent.html

O sistema de coordenadas utilizado pelo sensor do Android é definido em relação à tela do telefone em sua orientação padrão (modo retrato). Os eixos não são alterados quando a orientação da tela do dispositivo muda.

O eixo X é horizontal e aponta para a direita, o eixo Y é vertical e aponta para cima e o eixo Z aponta para o lado de fora da face frontal da tela. Neste sistema, as coordenadas atrás da tela têm valores negativos de Z.

![](https://i.imgur.com/j042mjz.png)

O sensor retorna os valores registrados em um vetor. O comprimento e o conteúdo desse vetor dependem de qual tipo de sensor está sendo monitorado. No caso do acelerômetro, o sensor mede a aceleração aplicada ao dispositivo e retorna os valores dos três eixos em variáveis do tipo float e na unidade do Sistema Internacional, m/s².

Como a força da gravidade sempre está influenciando na medição da aceleração do dispositivo, quando o dispositivo está em repouso em uma mesa, o acelerômetro lê um valor de 9,81 m/s² (aproximadamente o valor da força da gravidade). 

## Implementação

Como o acelerômetro do dispositivo retorna sempre os últimos valores registrados de cada eixo e precisamos dessa medição em tempo real, podemos calcular a variação da aceleração de cada eixo por meio da fórmula Δa = a₂ - a₁ onde a₁ é a última aceleração registrada e a₂ é a aceleração atual. Portanto, se o dispositivo estiver em repouso em uma mesa, ao invés de registrar 9,81 m/s², ele irá registrar 0,00 m/s², visto que não houve variação de aceleração pelo fato do dispositivo estar em repouso.

O próximo passo é guardar essas informações em memória. Por isso, o usuário pode definir um período de tempo no qual ele deseja que as informações sejam salvas (de 10 em 10 segundos, de 30 em 30 segundos). Ele também pode apagar as informações já salvas ou pausar o registro desses dados. Os dados são registrados com data e hora de registro (no formato DD/MM HH:MM:SS) e com os valores de aceleração de cada eixo do dispositivo.

## Screenshots

![](https://i.imgur.com/7nj9MCJ.png)
