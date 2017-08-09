# chatP2P
### Desenvolver um aplicativo de troca de mensagens instantâneas (chat) P2P com as seguintes funcionalidades:
  #### Descoberta de usuários on-line na mesma sub-rede através do algoritmo:
  #### Envia datagramas UDP de sonda contendo o seu nome de usuário e status para todos os endereços da sub-rede através do endereço de broadcast da sub-rede.
  #### Quando algum endereço enviar também uma mensagem de sonda, cadastrar ele na lista de usuários on-line, com o seu devido status;
  #### Cada cliente de chat ao receber uma mensagem de radar (OLA), deve:
  * cadastrar o usuário recebido na sua lista de usuários on-line, se ele já não existir;
  * atualizar o status do usuário;
  * ignorar mensagens de radar recebidas de si próprio;
  #### Os clientes deverão implementar uma função onde são enviadas mensagens de radar periódicos (cada 5s) em broadcast.
  #### Sempre que ficar sem receber mensagens de radar de um usuário por tempo superior a 30s, este usuário deve ser retirado da lista de usuários on-line.
#### Para qualquer usuário on-line é possível abrir um diálogo de chat. Este diálogo deve ser confiável e com entrega garantida.
#### É possível manter sessões de chat simultâneas com vários usuários através de janelas diferentes.
#### Quando houver perda de conexão com o usuário de chat deverá ser automaticamente fechada a janela daquela sessão.
#### Para implementação da interface pode ser utilizado o esqueleto em Java-Swing fornecido.
#### As mensagens de radar devem ser enviadas pela porta “5555”, e as sessões de chat devem ser abertas na porta “5556”.

### LAYOUT DAS MENSAGEMS:

Mensagem de SONDA (apresentação)
“OLA \n
usuário=<nomeusuario>,<status> \n\n”

* Obs: Ajustar máscara de rede.

![chat](https://user-images.githubusercontent.com/12532889/29143205-b398bf72-7d2a-11e7-9be6-d0c061d52c50.png)

