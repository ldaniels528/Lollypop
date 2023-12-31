node.api('/api/shocktrade/contests', {
    //////////////////////////////////////////////////////////////////////////////////////
    // creates a new contest
    // www post 'http://{{host}}:{{port}}/api/shocktrade/contests' <~ { name: "Winter is coming" }
    //////////////////////////////////////////////////////////////////////////////////////
    post: (name: String) => {
        val result = insert into Contests (name) values ($name)
        inserted_id('Contests', result)
    },

    //////////////////////////////////////////////////////////////////////////////////////
    // retrieves a contest
    // www get 'http://{{host}}:{{port}}/api/shocktrade/contests?id=aa440939-89cb-4ba1-80b6-20100ba6a286'
    //////////////////////////////////////////////////////////////////////////////////////
    get: (id: UUID) => {
        from ns('Contests') where contest_id is $id limit 1
    },

    //////////////////////////////////////////////////////////////////////////////////////
    // updates a contest
    // www put 'http://{{host}}:{{port}}/api/shocktrade/contests' <~ { id: "0a3dd064-b3c7-4c44-aad0-c7bd94e1f929", name: "Winter is coming" }
    //////////////////////////////////////////////////////////////////////////////////////
    put: (id: UUID, newName: String) => {
        val result = update Contests set name = $newName where contest_id is $id
        result.updated
    },

    //////////////////////////////////////////////////////////////////////////////////////
    // deletes a contest
    //////////////////////////////////////////////////////////////////////////////////////
    delete: (id: UUID) => {
        delete from Contests where contest_id is $id
    }
})

node.api('/api/shocktrade/contests/by/name', {
    //////////////////////////////////////////////////////////////////////////////////////
    // searches for contests by name
    // www post 'http://{{host}}:{{port}}/api/shocktrade/contests/by/name' <~ { searchText: "Winter" }
    //////////////////////////////////////////////////////////////////////////////////////
    post: (searchText: String) => {
        from ns('Contests') where name contains searchText
    }
})