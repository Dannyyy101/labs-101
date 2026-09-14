import pandas as pd
import requests

df = pd.read_excel("../data/BLS_4_0_2025_DE/BLS_4_0_Daten_2025_DE.xlsx")[["BLS Code", "Lebensmittelbezeichnung", "ENERCC Energie (Kilokalorien) [kcal/100g]", "WATER Wasser [g/100g]", "PROT625 Protein (Nx6,25) [g/100g]", "FAT Fett [g/100g]", "CHO Kohlenhydrate, verfügbar [g/100g]", "FIBT Ballaststoffe, gesamt [g/100g]"]]

df.rename(columns={'BLS Code':'blsCode', 'Lebensmittelbezeichnung':'name', 'ENERCC Energie (Kilokalorien) [kcal/100g]': "kcal", "WATER Wasser [g/100g]": "water", "PROT625 Protein (Nx6,25) [g/100g]": "protein", "FAT Fett [g/100g]": "fat", "CHO Kohlenhydrate, verfügbar [g/100g]": "carbohydrates", "FIBT Ballaststoffe, gesamt [g/100g]": "fiber"}, inplace=True)

headers = {'Content-type': 'application/json'}


for index, row in df.iterrows():
    url = 'http://localhost:8080/api/foods'
    row = row.replace('<LOD', None).replace('<LOD or <LOQ', None).replace("-", None).replace("TR", None).replace("<LOQ", None)
    response = requests.post(url, data=row.to_json(), headers=headers)
    print(row.to_json())
    if not response.ok:
        print(response)
        break