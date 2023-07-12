-- the input.csv file was generated with:
select 'species', 'genus', 'familia', 'ordo' union all
select Artname,Genus,Familia,Ordo
from main,system where main.species=system.Artname
and recording_date < '1960-01-01'
and recording_date > '1950-01-01'
group by main.species limit 101 -- one more for the header line
into outfile '/tmp/input.csv' fields terminated by ';' optionally enclosed by '"' lines terminated by '\n';
