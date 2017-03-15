package br.ufsm.csi.redes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * User: Vagner
 * Date: 25/11/16
 * Time: 08:30
 * 
 */

/*
Matrícula: 201321762 - Trabalho de Redes de Computadores
CHAT P2P:
Tarefas:
- Fazer a interface; Está OK
- Cadastrar usuários na lista lista de on-line; Está OK (Feito em Thread Separada)
- Retirar usuário da lista de on-line por timeout; Está OK (Feito Thread Separada)
- Validação da lista de usuários on-line e janelas que podem ser abertas; Está OK - Mas com alguns Problemas 
- Quando clicar no usuário on-line estabelecer conexão TCP por chat; (Thread Separada) Está OK - Mas com alguns Problemas 
    - Fechar janela; OK
    - Envio de mensagem; OK
    - Recebeimento de mensagem; OK
- Quando recebe nova conexão, abre a janela para ambos; (Thread Separada) - OK com alguns problemas

*/
public class ChatClientSwing extends JFrame {

    private Usuario meuUsuario; // Usuário Chat 
    private final String endBroadcast = "255.255.255.255";
    private JList listaChat;
    private DefaultListModel dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    final ArrayList <Usuario> usuarioOnline= new ArrayList(); // ArrayList com os Usuários que estão Onlines no Chat em mesma rede
    final ArrayList <Usuario> usuariosChatsAbertos= new ArrayList(); // ArrayList com os Usuários que estão em chat aberto
    private PainelChatPVT painelMensagem;
    private Set<String> chatsAbertos = new HashSet<>();

    
    private class ThreadEnviaSonda implements Runnable { // Thread que envia sondas pelo usuário 

        @Override
        public void run() {
            try {
                // DatagramSocket = Envia e recebe os pacotes de dados
                DatagramSocket ds = new DatagramSocket();
                
                while(true) { // Continua a rodar enquanto o Usuário estiver Online
                    try {
                        // InetAddress = Cria um objeto para conter o endereço IP utilizado.
                        // DatagramPacket = Contém byte, buffer, Endereço e porta.
                        
                        String sonda = "OLÁ \n"
                            + "usuário=" +meuUsuario.getNome()+ "," +meuUsuario.getStatus().toString(); // Mensagem do Usuário, Nome+Status do momento 
                        byte[] buf = sonda.getBytes(); // Array de Bytes com os dados da mensagem sonda que será mandado para outros usuários da rede
                        DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(endBroadcast), 5555); // Monta pacote, contento o array de bytes de mensagem, o tamanho do array
                        ds.send(dp); // Envia Sonda atravéns do ds.send contendo o pacote dp
                        Thread.sleep(1000); // Thread dorme por 1 segundo
                    } catch (Exception e) {
                        e.printStackTrace();
                    }                    
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
    private class ThreadRecebeSonda implements Runnable { // Thread que recebe Sondas que estão sendo enviadas pelos outros usuários da rede

        @Override
        public void run() {
            try {
                
                DatagramSocket ds = new DatagramSocket(5555); // Socket instanciado na Porta 5555       
                
                while (true) {
                    
                    byte[] buf = new byte[1024]; // Buffer do que é recebido
                    DatagramPacket dp = new DatagramPacket(buf, buf.length); // Instancia DatagramPacket
                    
                    try {
                        
                        ds.receive(dp); // Recebe Pacotes Sonda
                        System.out.println("[Pacote Sonda Recebido] - Usuário de Endereço: "+dp.getAddress().getHostAddress()); // Sonda Pacote Recebido, mostrando o Endereço de que usuário Enviou
                        // System.out.println(new String(buf, 0, dp.getLength())); // Imprime Conteúdo da Sonda
                        String sonda = new String(buf, 0, dp.getLength()); // Conteúdo da Sonda sendo transformada em String
                        System.out.println(sonda); // Imprime Sonda
                        System.out.println("\n");
                        
                        String usuario = sonda.substring(sonda.indexOf("=") + 1, sonda.indexOf(",")); // Separa o nome do Usuário do resto da mensagem da sonda recebida
                        // System.out.println(usuario);                        
                        
                        String status = sonda.substring(sonda.indexOf(",") + 1, sonda.length()); // Separa o status do Usuário do resto da mensagem da sonda recebida
                        // System.out.println(status);
                        
                        Usuario u = buscaUsuario(dp.getAddress()); // Busca usuário pelo endereço da sonda recebida e o instancia como Objeto usuário
                        
                        if(u == null) { // Se o usuário for nulo é por que ele não está na Lista tipo defaultlistModel utilizada pelo chat
                          
                            Usuario user = new Usuario(usuario, StatusUsuario.DISPONIVEL, dp.getAddress()); // Seta  Novo Usuário usando o construtor, com status de disponível, ou seja, significa que o usuário entra pela primeira vez no Chat
                            user.setDataUltimoPacote(new Date()); // Seta o tempo online, começo deste usuário no chat, guardando o horário do último pacote para futuras verificações de se está online
                            
                            dfListModel.addElement(user); // Adiciona o usuário na na lista usuários do tipo defaultlistModel 
                            usuarioOnline.add(user); // Adiciona o usuário na lista de usuários do tipo arrayList
                            
                        }else{ // Se o usuário já estiver na Lista de usuários do tipo defaultlistModel
                            
                            u.setStatus(StatusUsuario.valueOf(status)); // Seta o Status do Usuário 
                            u.setDataUltimoPacote(new Date()); // Seta a nova data do Ultimo Pacote recebido
                            
                            atualizaUsuario(u); // Atualiza o usuário na lista ditpo dfListModel
                            
                            System.out.println("");
                            System.out.println("Usuarios Online: ");
                            
                            for(int i=0; i<usuarioOnline.size(); i++){ // Percorre todo array de usuários    
                                
                                System.out.println("Nome: "+usuarioOnline.get(i).getNome()+", Endereco: "+usuarioOnline.get(i).endereco);
                                
                                if(usuarioOnline.get(i).endereco.equals(u.getEndereco())){// Condição para usuário de mesmo com endereço
                                   usuarioOnline.get(i).setDataUltimoPacote(new Date()); // Armazena o tempo de inicío do usuário na lista tipo array
                                }
                                
                            }
                            System.out.println("");
                            
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    
                }
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
        } 
    }
    
    private Usuario buscaUsuario(InetAddress address) { // Busca Usuário de acordo com o endereço na Lista de Usuários do tipo defaultlistModel
        
        Enumeration e = dfListModel.elements();
        
        while(e.hasMoreElements()) { // Enquanto houver usuários 
            Usuario user = (Usuario) e.nextElement(); // Instancia usuário do elemento da lista
            
            if(user.getEndereco().equals(address)) { // Se houver usuário com mesmo endereço na lista
                return user; // Retorna o usuário de mesmo endereço
            }
        }
        return null;
    }
        
    private void atualizaUsuario(Usuario u) { // Atualiza usuário da lista de tipo defaultlistModel de acordo com o Usuário passado por parâmetro
        
        int idx = dfListModel.indexOf(u); // Recebe o index (posição) do usuário na lista
        dfListModel.setElementAt(u, idx); // Seta usuário
        
    }
    
    private void iniciaTarefasBackground() { // Inicio de todas as threads necessárias, que executam em background
        
        new Thread(new ThreadEnviaSonda()).start(); // Thread que envia sondas para manter o usuário ativo no chat
        new Thread(new ThreadRecebeSonda()).start(); // Thread que recebe sondas de usuários 
        
        new Thread(new ThreadVerificaListaDeUsuarios()).start(); // Verifica Usuários que estão online no momento no Chat
        new Thread(new ThreadRecebeConversa()).start(); // Verifica a questão de 
    }
    
    private class ThreadVerificaListaDeUsuarios implements Runnable { // Verifica Usuários que estão online no momento no Chat
        @Override
        public void run() {              
            while (true) {
                
                ArrayList<Usuario> usuariosParaRemover = new ArrayList<Usuario>(); // Lista de Usuários para Remoção de acordo com o tempo
                Enumeration e = dfListModel.elements(); // Lista de Usuários
                
                while(e.hasMoreElements()) { // While que percorre a lista de usuários do tipo defaultlistModel
                    
                    Usuario user = (Usuario) e.nextElement(); // Instancia objeto usuário do elemento da lista
                    Date dataAtual = new Date(); // Data para verificar o tempo atual com o tempo do último pacote
                    Long tempo = dataAtual.getTime() - user.getDataUltimoPacote().getTime(); // Tempo atual - tempo do último pacote
                    
                    if (tempo > 5000) { // // Se o tempo entre o tempo atual e o tempo do último pacote recebido for maior que 10 segundos
                        usuariosParaRemover.add(user); // Usuário é colocado na lista de usuários a serem removidos
                        System.out.println("Usuário "+user.getNome()+" Removido"); 
                    }
                } 
                
                for (Usuario u : usuariosParaRemover) { // Foreach da lista de usuários serem removidos
                    dfListModel.removeElement(u); // Remove usuário da lista dfListModel
                }
                usuariosParaRemover = null;
                
                /* System.out.println("");
                System.out.println("Usuários Chats Abertos: ");
                for(int i=0; i<usuariosChatsAbertos.size(); i++){
                    System.out.println("Usuário "+usuariosChatsAbertos.get(i).getNome()); 
                } */
                
                try{
                    Thread.sleep(1000); // Dorme por 1 segundo
                }catch (InterruptedException ex){
                    ex.printStackTrace();
                }
                
            }
        } 
    }
    
    private class  ThreadRecebeConversa implements Runnable{ // Thread que Recebe conversas de outros usuários
        public void run() {
            
            try {
                
                ServerSocket serv = new ServerSocket(5556); // Aguarda mensagens na Porta 5556
                
                while (true) {                    
                    try {                        
                        Socket sock= serv.accept(); // Recebe mensagem da porta 5556
                       
                        System.out.println("Chat P2P recebendo mensagem...");
                        InetAddress endereco = sock.getInetAddress(); // Pega o endereço do usuário
                        Usuario u = buscaUsuario(endereco); // Busca usuário do endereço da mensagem
                        
                        String end = u.getEndereco().getHostAddress(); // Endereço do Usuário que vai inicar conexão
                        
                        if(end.equals(meuUsuario.getEndereco().getHostAddress())){ // Verifica os endereços, não podem ser iguais, visto que seria conversa consigo mesmo
                            JOptionPane.showMessageDialog(rootPane, "Conversa consigo mesmo não permitido!");
                        }
                        else{ // Endereços diferentes, logo é possível haver a conversa. Evita de a pessoa conversar consigo mesmo
                            PainelChatPVT painel = new PainelChatPVT(sock , u);
                            System.out.println("PAINELCHATPVT: "+u.nome);
                            // Cria espaço para conversas com outro usuário
                            tabbedPane.add(u.getNome(), painel); // Inicia conexão
                            usuariosChatsAbertos.add(u); // Adiciona Usuário nos Chats Abertos
                        }
                                
                    } catch (Exception e) {
                        
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ChatClientSwing.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ChatClientSwing() throws UnknownHostException { // Método de preparação dos componentes Chat Java desktop 
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.DISPONIVEL);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);
        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) { // Evento com Mouse de fechar Janela do Chat com Usuário
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    JMenuItem item = new JMenuItem("Fechar");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // PainelChatPVT painel = (PainelChatPVT) tabbedPane.getTabComponentAt(tab);
                            String elem = tabbedPane.getTitleAt(tab); 
                            tabbedPane.remove(tab); // Remove Aba
                            chatsAbertos.remove(elem); // Remove dos chats abertos 
                            
                            System.out.println("Elemento: "+elem);
                            // System.out.println("Usuário Fechado: "+painel.usuario.getNome());
                        }
                    });
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Trabalho Avaliativo - Chat P2P - Disciplina: Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuário para entrar no Chat: "); // Janela de preenchimento de Nome
        
        this.meuUsuario = new Usuario(nomeUsuario, StatusUsuario.DISPONIVEL, InetAddress.getLocalHost()); // Seta Usuário
        
        setVisible(true);
        iniciaTarefasBackground(); // Inicia as threads necessárias para manter o usuário online e para conversas
    }

    private JComponent criaLista() { // 
        dfListModel = new DefaultListModel();
        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                
                if (evt.getClickCount() == 2) { // Evento ao clicar em cima do nome do Usuário na Lista de usuários Online
                    
                    int index = list.locationToIndex(evt.getPoint()); // Index utilizado para buscar o Usuário na Lista Jlist
                    Usuario user = (Usuario)list.getModel().getElementAt(index); // Instanciação do objeto Usuário através do index
                    System.out.println("User To String ao clicar no nome do Usuário: "+user.toString());
                    System.out.println("");
                    
                    Socket s = null;
                                       
                    try {
                        s = new Socket(user.getEndereco().getHostAddress(), 5556); // Instanciação de socket com endereço do usuário e porta
                        String endereco = user.getEndereco().getHostAddress();
                        
                        if(endereco.equals(meuUsuario.getEndereco().getHostAddress())){ // Verifica se o endereço do Usuário é o mesmo do Usuário que realizará a conversa
                            // JOptionPane.showMessageDialog(rootPane, "Conversa consigo mesmo não permitido!");
                        }else{ // Endereços diferentes, logo é possível haver a conversa. Evita de a pessoa conversar consigo mesmo
                           if (chatsAbertos.add(user.toString())) { 
                            System.out.println("ABA: "+user.getNome());
                            // tabbedPane.add(user.toString(), new PainelChatPVT(s , meuUsuario));
                            tabbedPane.add(user.getNome(), new PainelChatPVT(s , user)); // ABRE PAINEL mandando o usuário a ser conversado
                            }
                        }
                        
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(ChatClientSwing.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(rootPane, "Erro do Chat!"); // Causado por exception
                    }
                    
                }
            }
        });
        return listaChat;
    }
    
    private class ThreadRecebeMensagem implements Runnable {

        private PainelChatPVT painel;
        private Socket s;
        
        public ThreadRecebeMensagem(PainelChatPVT painel, Socket s) { // painel e socket com o usuário como parâmetros
            this.painel = painel;
            this.s=s;
        }
        
        @Override
        public void run() {
            try {
               while(true){
                   byte[] msg = new byte[1024]; // Tamanho da mensagem
                   int tamanhoMSG = s.getInputStream().read(msg); // Lê mensagem no socket
                   String texto = new String(msg, 0 , tamanhoMSG); // Converte para texto
                   // painel.areaChat.append(painel.usuario.getNome()+ " (" + painel.usuario.status + ") disse: "+ texto+" \n");
                   painel.areaChat.append(painel.usuario.getNome()+" disse: "+ texto+" \n"); // Mensagem recebida de outro usuário
                   painelMensagem = painel;
               }
            }catch (Exception e) { JOptionPane.showMessageDialog(painel, "Usuário "+painel.usuario.getNome()+" Offline no Chat");}
            tabbedPane.remove(painel);
        }
        
    }

    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        Socket socket;
        
        PainelChatPVT(Socket s, Usuario usuario) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            this.socket = s;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            campoEntrada.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        socket.getOutputStream().write(e.getActionCommand().getBytes());
                        
                    } catch (IOException ex) {
                        Logger.getLogger(ChatClientSwing.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    ((JTextField) e.getSource()).setText("");
                    // areaChat.append(meuUsuario + ": " + e.getActionCommand() + "\n");
                    areaChat.append(meuUsuario.getNome() + ": " + e.getActionCommand() + "\n");
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            new Thread( new ThreadRecebeMensagem(this, s)).start(); // Inicia Thread de verificação recebimento de mensagens
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public void setUsuario(Usuario usuario) {
            this.usuario = usuario;
        }
    }

    public static void main(String[] args) throws UnknownHostException { // Primeiro método a ser executado
        new ChatClientSwing(); 
    }

    public enum StatusUsuario { // Status disponíveis para o Usuário.
        DISPONIVEL, NAO_PERTURBE, VOLTO_LOGO
    }

    // private Map<String, Usuario> usuariosOnline = new HashMap<String, Usuario>();

    public class Usuario { // Classe Usuário e seus atributos
        
        // Usuário necessita de seu nome, seu status, o endereço para identificação
        private String nome;
        private StatusUsuario status;
        private InetAddress endereco;
        private Date dataUltimoPacote; // último pacote para verificação de sonda

        public Usuario(String nome, StatusUsuario status, InetAddress endereco) {
            this.nome = nome;
            this.status = status;
            this.endereco = endereco;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public InetAddress getEndereco() {
            return endereco;
        }

        public void setEndereco(InetAddress endereco) {
            this.endereco = endereco;
        }
        
        public Date getDataUltimoPacote() {
            return dataUltimoPacote;
        }

        public void setDataUltimoPacote(Date dataUltimoPacote) {
            this.dataUltimoPacote = dataUltimoPacote;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Usuario usuario = (Usuario) o;

            return nome.equals(usuario.nome);

        }

        @Override
        public int hashCode() {
            return nome.hashCode();
        }

        public String toString() {
            return this.getNome() + " (" + getStatus().toString() + ")";
        }
    }

}
