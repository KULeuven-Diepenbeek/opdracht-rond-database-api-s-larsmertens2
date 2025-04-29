package be.kuleuven;

import java.util.List;

import org.jdbi.v3.core.Jdbi;

public class SpelerRepositoryJDBIimpl implements SpelerRepository {
  private Jdbi jdbi;

  // Constructor
  SpelerRepositoryJDBIimpl(String connectionString, String user, String pwd) {
    this.jdbi = Jdbi.create(connectionString, user, pwd);
  }


  
  @Override
  public void addSpelerToDb(Speler speler) {
    jdbi.withHandle(handle -> {
      return handle.execute("INSERT INTO speler (tennisvlaanderenid, naam, punten) VALUES (?, ?, ?);",
          speler.getTennisvlaanderenId(), speler.getNaam(), speler.getPunten());
    });
  }

  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
      Speler speler = jdbi.withHandle(handle ->
          handle.createQuery("SELECT * FROM speler WHERE tennisvlaanderenid = :id")
              .bind("id", tennisvlaanderenId)
              .mapToBean(Speler.class)
              .findOne()
              .orElse(null)
      );
      if (speler == null) {
          throw new RuntimeException("Invalid Speler met identification: " + tennisvlaanderenId);
      }
      return speler;
  }

  @Override
  public List<Speler> getAllSpelers() {
      return jdbi.withHandle(handle ->
          handle.createQuery("SELECT * FROM speler")
                .mapToBean(Speler.class)
                .list()
      );
  }

  @Override
  public void updateSpelerInDb(Speler speler) {
      int rowsAffected = jdbi.withHandle(handle -> 
          handle.createUpdate("UPDATE speler SET naam = :naam, punten = :punten WHERE tennisvlaanderenid = :id")
                .bind("naam", speler.getNaam())
                .bind("punten", speler.getPunten())
                .bind("id", speler.getTennisvlaanderenId())
                .execute()
      );
      if (rowsAffected == 0) {
          throw new RuntimeException("Invalid Speler met identification: " + speler.getTennisvlaanderenId());
      }
  }

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenid) {
      int rowsAffected = jdbi.withHandle(handle -> 
          handle.createUpdate("DELETE FROM speler WHERE tennisvlaanderenid = :id")
                .bind("id", tennisvlaanderenid)
                .execute()
      );
      if (rowsAffected == 0) {
          throw new RuntimeException("Invalid Speler met identification: " + tennisvlaanderenid);
      }
  }
  
  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenid) {
      return jdbi.withHandle(handle ->
          handle.createQuery("""
              SELECT t.clubnaam, w.finale, w.winnaar
              FROM wedstrijd w
              JOIN tornooi t ON w.tornooi = t.id
              WHERE (w.speler1 = :id OR w.speler2 = :id)
                AND w.finale IS NOT NULL
              ORDER BY w.finale ASC
              LIMIT 1
          """)
          .bind("id", tennisvlaanderenid)
          .map((rs, ctx) -> {
              String clubnaam = rs.getString("clubnaam");
              int finale = rs.getInt("finale");
              int winnaar = rs.getInt("winnaar");
  
              String finaleString;
              if (finale == 1) {
                  finaleString = (winnaar == tennisvlaanderenid) ? "winst" : "finale";
              } else if (finale == 2) {
                  finaleString = "halve finale";
              } else if (finale == 4) {
                  finaleString = "kwart finale";
              } else {
                  finaleString = "lager dan kwart finale";
              }
  
              return "Hoogst geplaatst in het tornooi van " + clubnaam + " met plaats in de " + finaleString;
          })
          .findOne()
          .orElse("Geen ranking gevonden voor speler met ID " + tennisvlaanderenid)
      );
  }
  

  @Override
  public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId) {
      jdbi.useHandle(handle ->
          handle.createUpdate("INSERT INTO speler_speelt_tornooi (speler, tornooi) VALUES (:speler, :tornooi)")
                .bind("speler", tennisvlaanderenId)
                .bind("tornooi", tornooiId)
                .execute()
      );
  }

  @Override
  public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
      jdbi.useHandle(handle ->
          handle.createUpdate("DELETE FROM speler_speelt_tornooi WHERE speler = :speler AND tornooi = :tornooi")
                .bind("speler", tennisvlaanderenId)
                .bind("tornooi", tornooiId)
                .execute()
      );
  }
}
