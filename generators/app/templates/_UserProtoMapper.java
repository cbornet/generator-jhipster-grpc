package <%=packageName%>.grpc;

import <%=packageName%>.domain.User;
import <%=packageName%>.service.dto.UserDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.HashSet;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class UserProtoMapper extends ProtobufUtil {

    public UserDTO userProtoToUserDTO(UserProto userProto) {
        if ( userProto == null ) {
            return null;
        }
        return new UserDTO(
            "".equals(userProto.getId()) ? null : userProto.getId(),
            "".equals(userProto.getLogin()) ? null : userProto.getLogin(),
            "".equals(userProto.getFirstName()) ? null : userProto.getFirstName(),
            "".equals(userProto.getLastName()) ? null : userProto.getLastName(),
            "".equals(userProto.getEmail()) ? null : userProto.getEmail(),
            userProto.getActivated(),<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>
            "".equals(userProto.getImageUrl()) ? null : userProto.getImageUrl(),<% } %>
            "".equals(userProto.getLangKey()) ? null : userProto.getLangKey(),<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>
            "".equals(userProto.getCreatedBy()) ? null : userProto.getCreatedBy(),
            timestampToZonedDateTime(userProto.getCreatedDate()),
            "".equals(userProto.getLastModifiedBy()) ? null: userProto.getLastModifiedBy(),
            timestampToZonedDateTime(userProto.getLastModifiedDate()),<% } %>
            new HashSet<>(userProto.getAuthoritiesList())
        );
    }

    public UserProto userDTOToUserProto(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        UserProto.Builder builder = userDTOToUserProtoBuilder(userDTO);
        // Authorities were not mapped because the method name doesn't match javabean setter names.
        if (userDTO.getAuthorities() != null) {
            builder.addAllAuthorities(userDTO.getAuthorities());
        }
        return builder.build();
    }

    UserProto.Builder createUserProto () {
        return UserProto.newBuilder();
    }

    abstract UserProto.Builder userDTOToUserProtoBuilder (UserDTO userDTO);

    public UserProto userToUserProto(User user) {
        if (user == null) {
            return null;
        }
        return userToUserProtoBuilder(user).build();
    }

    @Mapping(target = "password", ignore = true)
    abstract UserProto.Builder userToUserProtoBuilder (User user);

}
