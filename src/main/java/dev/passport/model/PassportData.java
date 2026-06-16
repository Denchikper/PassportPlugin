package dev.passport.model;

import java.util.UUID;

/**
 * Модель данных паспорта одного игрока.
 * <p>
 * Простой изменяемый POJO: загружается из базы, редактируется командами/API,
 * сохраняется обратно через {@link dev.passport.database.DatabaseManager}.
 */
public class PassportData {

    private final UUID uuid;

    private String rpName;          // РП-имя персонажа
    private String birthDate;       // Дата рождения, напр. "15.03.1995"
    private String birthPlace;      // Место рождения
    private String maritalStatus;   // Семейное положение
    private String profession;      // Профессия/работа
    private String citizenship;     // Гражданство/город
    private String spouseName;      // РП-имя супруга/супруги

    /**
     * Создаёт пустой паспорт для указанного игрока.
     * Все текстовые поля инициализируются пустой строкой, чтобы избежать null.
     *
     * @param uuid UUID владельца паспорта
     */
    public PassportData(UUID uuid) {
        this.uuid = uuid;
        this.rpName = "";
        this.birthDate = "";
        this.birthPlace = "";
        this.maritalStatus = "";
        this.profession = "";
        this.citizenship = "";
        this.spouseName = "";
    }

    /**
     * Полный конструктор — используется при загрузке из базы данных.
     */
    public PassportData(UUID uuid,
                        String rpName,
                        String birthDate,
                        String birthPlace,
                        String maritalStatus,
                        String profession,
                        String citizenship,
                        String spouseName) {
        this.uuid = uuid;
        this.rpName = orEmpty(rpName);
        this.birthDate = orEmpty(birthDate);
        this.birthPlace = orEmpty(birthPlace);
        this.maritalStatus = orEmpty(maritalStatus);
        this.profession = orEmpty(profession);
        this.citizenship = orEmpty(citizenship);
        this.spouseName = orEmpty(spouseName);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRpName() {
        return rpName;
    }

    public void setRpName(String rpName) {
        this.rpName = orEmpty(rpName);
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = orEmpty(birthDate);
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = orEmpty(birthPlace);
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = orEmpty(maritalStatus);
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = orEmpty(profession);
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = orEmpty(citizenship);
    }

    public String getSpouseName() {
        return spouseName;
    }

    public void setSpouseName(String spouseName) {
        this.spouseName = orEmpty(spouseName);
    }

    @Override
    public String toString() {
        return "PassportData{" +
                "uuid=" + uuid +
                ", rpName='" + rpName + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", birthPlace='" + birthPlace + '\'' +
                ", maritalStatus='" + maritalStatus + '\'' +
                ", profession='" + profession + '\'' +
                ", citizenship='" + citizenship + '\'' +
                ", spouseName='" + spouseName + '\'' +
                '}';
    }
}