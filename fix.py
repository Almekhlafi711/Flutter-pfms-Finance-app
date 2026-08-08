import re
with open('app/src/main/java/com/example/data/repository/PfmsRepositoryImpl.kt', 'r') as f:
    c = f.read()
c = c.replace('DebtEntity("dbt_1", "Ahmed Al-Mansoor"', 'DebtEntity("dbt_1", "prs_1", "Ahmed Al-Mansoor"')
c = c.replace('DebtEntity("dbt_1", "prs_1", "prs_1", "Ahmed Al-Mansoor"', 'DebtEntity("dbt_1", "prs_1", "Ahmed Al-Mansoor"')
c = c.replace('DebtEntity("dbt_2", "Samba Auto Finance"', 'DebtEntity("dbt_2", "prs_2", "Samba Auto Finance"')
c = c.replace('DebtEntity("dbt_2", "prs_2", "prs_2", "Samba Auto Finance"', 'DebtEntity("dbt_2", "prs_2", "Samba Auto Finance"')
c = c.replace('DebtEntity("dbt_3", "Mohammed Al-Amri"', 'DebtEntity("dbt_3", "prs_3", "Mohammed Al-Amri"')
c = c.replace('DebtEntity("dbt_3", "prs_3", "prs_3", "Mohammed Al-Amri"', 'DebtEntity("dbt_3", "prs_3", "Mohammed Al-Amri"')
c = c.replace('DebtEntity("dbt_4", "Tariq Yemen Import"', 'DebtEntity("dbt_4", "prs_4", "Tariq Yemen Import"')
c = c.replace('DebtEntity("dbt_4", "prs_4", "prs_4", "Tariq Yemen Import"', 'DebtEntity("dbt_4", "prs_4", "Tariq Yemen Import"')
with open('app/src/main/java/com/example/data/repository/PfmsRepositoryImpl.kt', 'w') as f:
    f.write(c)
