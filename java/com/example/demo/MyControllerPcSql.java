package com.example.demo;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.mail.MessagingException;

@Controller
public class MyControllerPcSql {
	
	@Autowired
	EmailService emailService;

    pcJdbcTemplate d1;
    ArrayList<pc> listaPc = new ArrayList<>();
    ArrayList<pcComprati> pcComprati = new ArrayList<>();

    @Autowired
    public MyControllerPcSql(pcJdbcTemplate d1) {
        this.d1 = d1;
        this.listaPc=d1.getLista();
    }

    @GetMapping("/")
    public String getIndex(Model model) {
        String nome = "pc";
        model.addAttribute("nome", nome);
        return "index";
    }

    @GetMapping("/listaPcStore")
    public String getLista(Model model) {
        listaPc = d1.getLista();
        model.addAttribute("lista", listaPc);
        return "store";
    }

    @GetMapping("/vaiMagazzino")
    public String getMagazzino(Model model) {
        listaPc = d1.getLista();
        model.addAttribute("lista", listaPc);
        return "stampaMagazzino"; // Assicurati che questo corrisponda al file HTML
    }

    // Per inserire un prodotto nel magazzino
    @PostMapping("/gestioneMagazzino")
    public String getPc(@RequestParam("nome") String nome,
                        @RequestParam("marca") String marca,
                        @RequestParam("descrizione") String descrizione,
                        @RequestParam("prezzo") Double prezzo,
                        @RequestParam("url") String url,
                        @RequestParam("qntMagazzino") int qntMagazzino,
                        @RequestParam("qntVenduti") int qntVenduti, Model m1) {
        // Inserimento nel database
        d1.insertPc(nome, marca, descrizione, prezzo, url, qntMagazzino, qntVenduti);

        // Aggiungi il prodotto alla lista
        pc pcSingolo = new pc(nome, marca, descrizione, prezzo, url, qntMagazzino, qntVenduti);
        listaPc.add(pcSingolo);

        // Ottieni di nuovo la lista aggiornata
        listaPc = d1.getLista();
        m1.addAttribute("lista", listaPc);

        // Ritorna la pagina di visualizzazione del magazzino
        return "stampaMagazzino";  
    }
    
    //metodo per rimuovere un prodotto dal magazino. ovvero tutto 
//
//</form>
    @PostMapping("/rimuoviDalMagazzino")
    public String rimuoviDalMagazzino(@RequestParam("nome") String nome, Model m1) {
        // Rimuove il prodotto dalla lista in memoria
        listaPc.removeIf(pc -> pc.getNome().equals(nome));

        // RIMOZIONE del pc preso in base al nome tramite il metodo delete creato in jdbctemplate
        d1.delete(nome);  

        // Aggiorna la lista nel model
        m1.addAttribute("lista", listaPc);
        return "redirect:/vaiMagazzino";  //reindirizzo al caricamento del magazzino 
    }

    
    
    
   ////>>>>>>>>>>>>>>> metodi  per lo store
    
    
    //mappiamo il carrello //num è la quantita di prodotti . che mettero nelle card 
    
    //facciamo un reindirizzamento ad un altra funzione mappata con /carrello che mi stampa il carrello
    //cosi non abbiamo bisogni di fare il refresh della pagina ad ogni cambiamento del carrello 
    @PostMapping("/aggiungiCarrello")
    public String compra(@RequestParam("nome") String nome, @RequestParam("num") int num) {
    	
    	if(num>0)//abbiamo effettuato un acquisto
    		//itero per trovare il nome dell oggetto e la quantita 
    		for (pc  pc: listaPc) {
    			
    			if(pc.getNome().equals(nome)) {
    				//controlliamo se quei pc sono disponibili in magazzino
    				 if(pc.qntMagazzino>=num) {
    					
    	                    pcComprati pcAcquistato = new pcComprati(
    	                        pc.getNome(),
    	                        pc.getMarca(),
    	                        pc.getDescrizione(),
    	                        pc.getPrezzo(),
    	                        pc.getUrl(),
    	                        pc.getQntMagazzino(),
    	                        pc.getQntVenduti(),
    	                        num // quantità acquistata)
    	                        );
    	                    
    	                
    	                    pcComprati.add(pcAcquistato); // lo aggiungo al carrello dove viene visualizzato 
    				 }
    				
    			}
    		}
    	return "redirect:/funzioneCarrello";
    	 //return "redirect:/store";
    }
   
    //pero cosi rimuovo tutti  i pc che si chiamano cosi. forse è meglio usare gli indici con un for sugli indici dell'array pcComprati
    @PostMapping("/rimuoviDalCarrello")
    public String rimuoviDalCarrello(@RequestParam("nome") String nome, Model m1) {
        System.out.println("Rimuovendo dal carrello: " + nome);
        
        boolean removed = pcComprati.removeIf(pc -> pc.getNome().equals(nome));
        if (removed) {
            System.out.println(nome + " è stato rimosso dal carrello.");
        } else {
            System.out.println(nome + " non è stato trovato nel carrello.");
        }

        return "redirect:/funzioneCarrello";
    }

    
    //funzione per stamapare il carrello IMPORTANTE 
    @GetMapping("/funzioneCarrello")
    public String stampaCarrello(Model m1) {
        double somma = 0;
       
        
        
        ArrayList<pcComprati> listaCarrello = new ArrayList<>(); 
       listaCarrello.clear();
        for (pcComprati pc : pcComprati) {
            somma += pc.getPrezzo() * pc.getQnt();
            listaCarrello.add(pc); 
        }
        
        m1.addAttribute("lista", listaPc); //altrimenti scompare dallo store
        
        m1.addAttribute("somma", somma);
        m1.addAttribute("carrello", listaCarrello); // Passa la lista al template IMPORTANTE . "carrello" va nel div sidebare e nel th:ech
        return "store"; 
    }

    
    
    
//    @GetMapping("/funzioneCarrello")
//    public String stampaCarrello(Model m1) {
//    	double somma= 0;
//    	for(pcComprati  pc : pcComprati) {
//    		somma+= pc.getPrezzo()*pc.getQnt();
//    		// metodo getQnt  appartiene a pcComprati
//    	}
//    	m1.addAttribute("somma", somma);
//		m1.addAttribute("lista", pcComprati);
//		return("store");
//    		
//    	}
//    
    
    
   //metodo per fare update delle quantita di pc (sotto ogni card) da acquistare 
    //Aggiorna la quantità (num) di un prodotto già selezionato nel carrello, se la quantità in magazzino (qntMagazzino) lo permette.
    @PostMapping("/cambiaQnt")
    public String rimuovi
    	
    	(@RequestParam("nome") String nome, @RequestParam("num") int num) {
    		for(pc pc : listaPc) {
    			if(pc.getNome().equals(nome)) {
    				for(pcComprati pc1 : pcComprati ){
    					if (pc1.getNome().equals(nome) && pc.qntMagazzino >= num) {
    						pc1.setQnt(num);
    						break;
    					}
    				}				
    			}
    		}
    		return "redirect:/funzioneCarrello"; //non mi dirigo alla pagina store ma richiamo la funzioneCarrello che me la ristampa 
    		//evitando il refresh della pagina 
    	}
    
    
    /// metodo per rimuovere tutto dal carrello. nel carrello metto un bottone conil mapping svuotacarrello 
//    <form action="/svuotaCarrello" method="post">
//    <button type="submit">Svuota Carrello</button>
//</form>
    
    
    ///non usato 
    @PostMapping("/svuotaCarrello")
    public String svuotaCarrello() {
        pcComprati.clear(); // Rimuove tutti gli elementi dal carrello
        return "redirect:/funzioneCarrello"; // Reindirizza alla pagina del carrello vuoto
    }
    
    
    	
    ///CONFERMA ACQUISTO NEL CARRELLO 
    ///una funzione che al clic del bottone acquista reindirizza ad una pagina di avvenuto acquisto
    //è doppione della  dunzione stampaCarrello ma con un reindirizzamento ad una altra pagina 
    //nel crrello ci sara un bottone di confermaAcquisto con il mapping /confermaAcquisto
    @PostMapping("/confermaAcquisto")
     public String confermaAcquisto(Model m1) {
    	
    	 // Controlla se ci sono articoli nel carrello ovvero se la lista è nulla o vuota 
        if (pcComprati == null || pcComprati.isEmpty()) {
            
            m1.addAttribute("messaggio", "Il carrello è vuoto. Non puoi confermare l'acquisto.");
        	
    		m1.addAttribute("lista", listaPc); //ricarico il catalogo 
            return "store"; 
        }
        
        
        
        ///ma voglio aggiornare il db con la quantita di pc venduti e in magazino 
        //ma qui devo fare un UPDATE del database d1.updatePc(num.get(i), listaPc.get(i).nome)
        //mi serve nel pcJdbcTemplate un metodo update che mi cambia la quantita di pcVenduti e Acquistati
        //for(int 1=0; i<num.size(); i++{
        //
        // if(num.get(i)!=0)
       //  {
        //	 d1.updatePc(num.get(i), listaPc.getPC(i).mome)
        	 
        // }
        
        
    	
    	//PRENDE I DATI DEL CARRELLO OVVERO SOMMA E PCACQUISTATI E LI STAMPA NELLA PAGINA CONFERMAACQUISTO.HTML
        
    	double somma= 0;
    	for(pcComprati  pc : pcComprati) {
    		somma+= pc.getPrezzo()*pc.getQnt();
    		// metodo getQnt  appartiene a pcComprati
    	}
    	m1.addAttribute("somma", somma);
		m1.addAttribute("lista", pcComprati);
		
		
		return("confermaAcquisto");
    }
    	
    	
    	
    @ResponseBody
    @PostMapping("/Recap")
   public String getResoconto(@RequestParam("mail") String mail) throws MessagingException     {
	   
    	ArrayList<String> url = new ArrayList<>();
    	
        String recap = "";  
    	recap+="riepilogo acquisti ";
    	double somma= 0;
    	for(pcComprati  pc : pcComprati) {
    		somma+= pc.getPrezzo()*pc.getQnt();
    		recap+= pc+"  ,";
    		
    		url.add(pc.url);
    		// metodo getQnt  appartiene a pcComprati
    	}
    	
    	recap+= "Totale da pagare  :"+somma;
    	
    	emailService.sendEmailWithImage(mail, "mail dallo stor pc", recap, url);
    	
    	return("Acquisto avvenuto con successo");
    	
    	
    	
    }
    	
    	
    	
    }
    	
    	
    
    
    
    
    
    
    
    
    
    
    
























